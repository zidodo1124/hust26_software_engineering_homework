package cn.edu.hust.ddlsaver.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.edu.hust.ddlsaver.data.RoomEventRepository
import cn.edu.hust.ddlsaver.data.SaverDatabase
import cn.edu.hust.ddlsaver.domain.AppSettings
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.EventDraft
import cn.edu.hust.ddlsaver.domain.EventConflictException
import cn.edu.hust.ddlsaver.domain.EventNotFoundException
import cn.edu.hust.ddlsaver.domain.EventValidationException
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

data class SaverUiState(
    val loading: Boolean = true,
    val events: List<DdlEvent> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val now: Instant = Instant.now(),
    val loadError: String? = null,
    val operationError: String? = null,
    val busy: Boolean = false,
    val actionResult: ActionResult? = null,
)

/** One database per Activity ViewModel; rotation keeps the instance and its pending operations. */
class SaverViewModel(application: Application) : AndroidViewModel(application) {
    private val database = SaverDatabase.create(application)
    private val repository = RoomEventRepository(database)
    private val mutableState = MutableStateFlow(SaverUiState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private var nextActionToken = 0L

    init {
        reload()
        viewModelScope.launch {
            while (true) {
                refreshTime()
                // Align updates with minute boundaries without caching a stale clock after resume.
                delay(60_000L - Math.floorMod(System.currentTimeMillis(), 60_000L))
            }
        }
    }

    fun reload() {
        observation?.cancel()
        mutableState.update { it.copy(loading = true, loadError = null) }
        observation = viewModelScope.launch {
            try {
                combine(repository.events, repository.settings) { events, settings -> events to settings }
                    .collect { (events, settings) ->
                        mutableState.update {
                            it.copy(events = events, settings = settings, loading = false, loadError = null)
                        }
                    }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    it.copy(loading = false, loadError = "暂时无法读取本机数据，请重试。")
                }
            }
        }
    }

    fun refreshTime() = mutableState.update { it.copy(now = Instant.now()) }

    fun clearOperationError() = mutableState.update { it.copy(operationError = null) }

    fun acknowledgeAction(token: Long) = mutableState.update {
        if (it.actionResult?.token == token) it.copy(actionResult = null) else it
    }

    fun save(draft: EventDraft) = perform("save") {
        val saved = repository.save(draft)
        saved.id to saved.revision
    }

    fun setCompleted(event: DdlEvent) = perform("state") {
        repository.setCompleted(event.id, event.completedAt == null)
        event.id to null
    }

    fun setArchived(event: DdlEvent) = perform("state") {
        repository.setArchived(event.id, event.archivedAt == null)
        event.id to null
    }

    fun delete(event: DdlEvent) = perform("delete") {
        repository.delete(event.id)
        event.id to null
    }

    fun saveUrgentMinutes(minutes: Long) = perform("settings") {
        repository.setUrgentMinutes(minutes)
        null to null
    }

    private fun perform(kind: String, action: suspend () -> Pair<String?, Long?>) {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true, operationError = null) }
        viewModelScope.launch {
            try {
                val (id, revision) = action()
                mutableState.update {
                    it.copy(busy = false, actionResult = ActionResult(++nextActionToken, kind, id, revision))
                }
            } catch (cancelled: CancellationException) {
                mutableState.update { it.copy(busy = false) }
                throw cancelled
            } catch (error: Exception) {
                val message = if (error is EventValidationException ||
                    error is EventConflictException || error is EventNotFoundException
                ) {
                    error.message ?: "请检查填写内容后重试。"
                } else {
                    "操作未完成，请重试。当前填写的内容已保留。"
                }
                mutableState.update { it.copy(busy = false, operationError = message) }
            }
        }
    }

    override fun onCleared() {
        observation?.cancel()
        database.close()
        super.onCleared()
    }

    companion object {
        fun factory(application: Application) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(SaverViewModel::class.java))
                @Suppress("UNCHECKED_CAST")
                return SaverViewModel(application) as T
            }
        }
    }
}
