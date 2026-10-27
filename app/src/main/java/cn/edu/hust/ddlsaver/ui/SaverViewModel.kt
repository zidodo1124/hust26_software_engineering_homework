package cn.edu.hust.ddlsaver.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.edu.hust.ddlsaver.data.RoomEventRepository
import cn.edu.hust.ddlsaver.data.SaverDatabase
import cn.edu.hust.ddlsaver.domain.*
import cn.edu.hust.ddlsaver.platform.AttachmentStore
import cn.edu.hust.ddlsaver.platform.AttachmentException
import cn.edu.hust.ddlsaver.reminders.ReminderRuntime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class ActionResult(val token: Long, val kind: String, val eventId: String?, val savedRevision: Long? = null)
data class ReminderNavigation(val token: Long, val eventId: String?, val today: Boolean, val deferred: Boolean = false)

data class ImageImportResult(val token: Long, val session: Long, val attachments: List<AttachmentRef>)
data class SaverUiState(
    val loading: Boolean = true,
    val events: List<DdlEvent> = emptyList(),
    val sources: List<Category> = emptyList(),
    val tags: List<Category> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val now: Instant = Instant.now(),
    val loadError: String? = null,
    val operationError: String? = null,
    val cleanupWarning: String? = null,
    val busy: Boolean = false,
    val actionResult: ActionResult? = null,
    val imageImportResult: ImageImportResult? = null,
    val reminderNavigation: ReminderNavigation? = null,
    val justCompleted: Boolean = false,
)

/** Owns pending IO across Activity recreation; UI callbacks never own an import task. */
class SaverViewModel(application: Application) : AndroidViewModel(application) {
    private val database = SaverDatabase.create(application)
    private val repository = RoomEventRepository(database)
    private val attachments = AttachmentStore(application)
    private val reminders = ReminderRuntime.get(application)
    val reminderHealth = reminders.health
    private val mutableState = MutableStateFlow(SaverUiState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private var nextActionToken = 0L
    private var nextNavigationToken = 0L
    private var completionFeedbackJob: Job? = null
    private var policyJob: Job? = null
    private var orphanCleanupStarted = false
    private data class DraftFiles(val originals: List<AttachmentRef>, val candidates: MutableMap<String, AttachmentRef>)
    private val draftFiles = mutableMapOf<Long, DraftFiles>()
    private val finishedSessions = mutableSetOf<Long>()

    init {
        reload()
        viewModelScope.launch {
            while (true) {
                refreshTime()
                delay(60_000L - Math.floorMod(System.currentTimeMillis(), 60_000L))
            }
        }
    }

    fun reload() {
        observation?.cancel()
        mutableState.update { it.copy(loading = true, loadError = null) }
        observation = viewModelScope.launch {
            try {
                combine(repository.events, repository.settings, repository.sources, repository.tags) { events, settings, sources, tags ->
                    SaverUiState(loading = false, events = events, settings = settings, sources = sources, tags = tags)
                }.collect { loaded ->
                    mutableState.update { it.copy(events = loaded.events, settings = loaded.settings,
                        sources = loaded.sources, tags = loaded.tags, loading = false, loadError = null) }
                    reminders.requestReconcile()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(loading = false, loadError = "暂时无法读取本机数据，请重试。") } }
        }
    }

    fun refreshReminders() = reminders.requestReconcile()

    fun requestReminderNavigation(eventId: String?, today: Boolean) {
        if (eventId == null && !today) return
        mutableState.update { it.copy(reminderNavigation = ReminderNavigation(++nextNavigationToken, eventId, today)) }
    }

    fun deferReminderNavigation(token: Long) = mutableState.update {
        val request = it.reminderNavigation
        if (request != null && request.token == token) it.copy(reminderNavigation = request.copy(deferred = true)) else it
    }

    fun acknowledgeReminderNavigation(token: Long) = mutableState.update {
        if (it.reminderNavigation?.token == token) it.copy(reminderNavigation = null) else it
    }

    fun refreshTime() {
        reminders.requestReconcile()
        mutableState.update { it.copy(now = Instant.now()) }
        if (policyJob?.isActive == true) return
        policyJob = viewModelScope.launch {
            try { repository.applyExpiredPolicy() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutableState.update { it.copy(cleanupWarning = "自动归档暂未完成，将在下次刷新时重试。") } }
        }
    }

    fun registerDraft(session: Long, originals: List<AttachmentRef>, current: List<AttachmentRef>) {
        if (session in finishedSessions) return
        val files = draftFiles.getOrPut(session) { DraftFiles(originals, mutableMapOf()) }
        current.forEach { files.candidates[it.relativePath] = it }
    }

    fun cleanupOldAttachments() {
        if (orphanCleanupStarted) return
        orphanCleanupStarted = true
        viewModelScope.launch {
            try {
                val protected = draftFiles.values.flatMap { it.candidates.keys }.toSet()
                attachments.cleanupOrphans(repository.referencedAttachmentPaths(), protected)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { cleanupFailed() }
        }
    }

    fun importImages(session: Long, uris: List<Uri>) {
        if (mutableState.value.busy || mutableState.value.imageImportResult != null || uris.isEmpty()) return
        mutableState.update { it.copy(busy = true, operationError = null) }
        viewModelScope.launch {
            try {
                val imported = attachments.importImages(uris)
                val files = draftFiles[session]
                if (files == null) {
                    cleanCandidates(imported)
                    mutableState.update { it.copy(busy = false) }
                } else {
                    imported.forEach { files.candidates[it.relativePath] = it }
                    mutableState.update { it.copy(busy = false,
                        imageImportResult = ImageImportResult(++nextActionToken, session, imported)) }
                }
            } catch (cancelled: CancellationException) { mutableState.update { it.copy(busy = false) }; throw cancelled }
            catch (error: Exception) { mutableState.update { it.copy(busy = false,
                operationError = if (error is AttachmentException) error.message ?: "图片导入失败，请重新选择。"
                else "图片导入失败，请重新选择。") } }
        }
    }

    fun acknowledgeImages(token: Long) = mutableState.update {
        if (it.imageImportResult?.token == token) it.copy(imageImportResult = null) else it
    }

    fun discardDraft(session: Long) {
        finishedSessions.add(session)
        val files = draftFiles.remove(session) ?: return
        val originalPaths = files.originals.map { it.relativePath }.toSet()
        val candidates = files.candidates.values.filter { it.relativePath !in originalPaths }
        viewModelScope.launch { cleanCandidates(candidates) }
    }

    fun clearOperationError() = mutableState.update { it.copy(operationError = null) }
    fun reportError(message: String) = mutableState.update { it.copy(operationError = message) }
    fun acknowledgeCleanupWarning() = mutableState.update { it.copy(cleanupWarning = null) }
    fun acknowledgeAction(token: Long) = mutableState.update {
        if (it.actionResult?.token == token) it.copy(actionResult = null) else it
    }

    fun save(draft: EventDraft, session: Long) = perform("save") {
        val saved = repository.save(draft)
        finishedSessions.add(session)
        val files = draftFiles.remove(session)
        cleanCandidates(files?.let { it.originals + it.candidates.values } ?: emptyList())
        saved.id to saved.revision
    }
    fun setCompleted(event: DdlEvent) = perform("state", completionFeedback = event.completedAt == null) {
        repository.setCompleted(event.id, event.completedAt == null)
        if (event.completedAt != null) { completionFeedbackJob?.cancel(); mutableState.update { it.copy(justCompleted = false) } }
        event.id to null
    }
    fun setArchived(event: DdlEvent) = perform("state") { repository.setArchived(event.id, event.archivedAt == null); event.id to null }
    fun delete(event: DdlEvent) = perform("delete") {
        repository.delete(event.id)
        cleanCandidates(event.attachments)
        event.id to null
    }
    fun applyBatch(ids: Set<String>, operation: BatchOperation) {
        val frozen = ids.toSet()
        val oldAttachments = mutableState.value.events.filter { it.id in frozen }.flatMap { it.attachments }
        val newlyCompleted = operation == BatchOperation.COMPLETE && mutableState.value.events.any { it.id in frozen && it.completedAt == null }
        perform("batch", completionFeedback = newlyCompleted) {
            repository.applyBatch(frozen, operation)
            if (operation == BatchOperation.DELETE) cleanCandidates(oldAttachments)
            null to null
        }
    }
    fun archiveExpired() = perform("batch") { repository.archiveExpired(); null to null }
    fun saveSettings(settings: AppSettings) = perform("settings") {
        repository.saveSettings(settings)
        try { repository.applyExpiredPolicy() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { mutableState.update { it.copy(cleanupWarning = "设置已保存，过期事项暂未归档，将在下次刷新时重试。") } }
        null to null
    }
    fun saveCategory(isSource: Boolean, id: String?, name: String) = perform("category") {
        if (isSource) repository.saveSource(id, name) else repository.saveTag(id, name)
        null to null
    }
    fun setCategoryActive(isSource: Boolean, category: Category) = perform("category") {
        if (isSource) repository.setSourceActive(category.id, !category.isActive)
        else repository.setTagActive(category.id, !category.isActive)
        null to null
    }

    private suspend fun cleanCandidates(candidates: List<AttachmentRef>) {
        if (candidates.isEmpty()) return
        try { attachments.discardUnreferenced(candidates, repository.referencedAttachmentPaths()) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { cleanupFailed() }
    }
    private fun cleanupFailed() = mutableState.update { it.copy(cleanupWarning = "数据操作已完成，部分闲置图片暂未清理，将在后续启动时重试。") }
    private fun perform(kind: String, completionFeedback: Boolean = false, action: suspend () -> Pair<String?, Long?>) {
        if (mutableState.value.busy || mutableState.value.actionResult != null || mutableState.value.imageImportResult != null) return
        mutableState.update { it.copy(busy = true, operationError = null) }
        viewModelScope.launch {
            try {
                val (id, revision) = action()
                reminders.requestReconcile()
                if (completionFeedback) {
                    completionFeedbackJob?.cancel()
                    mutableState.update { it.copy(justCompleted = true) }
                    completionFeedbackJob = viewModelScope.launch {
                        delay(5_000)
                        mutableState.update { it.copy(justCompleted = false) }
                    }
                }
                mutableState.update { it.copy(busy = false, actionResult = ActionResult(++nextActionToken, kind, id, revision)) }
            } catch (cancelled: CancellationException) { mutableState.update { it.copy(busy = false) }; throw cancelled }
            catch (error: Exception) {
                val message = if (error is EventValidationException || error is EventConflictException || error is EventNotFoundException)
                    error.message ?: "请检查填写内容后重试。"
                else "操作未完成，请重试。当前填写的内容已保留。"
                mutableState.update { it.copy(busy = false, operationError = message) }
            }
        }
    }
    override fun onCleared() { observation?.cancel(); database.close(); super.onCleared() }
    companion object {
        fun factory(application: Application) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(SaverViewModel::class.java))
                @Suppress("UNCHECKED_CAST") return SaverViewModel(application) as T
            }
        }
    }
}
