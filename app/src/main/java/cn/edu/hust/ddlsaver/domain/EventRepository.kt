package cn.edu.hust.ddlsaver.domain

import kotlinx.coroutines.flow.Flow

interface EventRepository {
    val events: Flow<List<DdlEvent>>
    val settings: Flow<AppSettings>
    val sources: Flow<List<Category>>
    val tags: Flow<List<Category>>

    /** New drafts have no ID; edits must carry the revision originally opened by the editor. */
    suspend fun save(draft: EventDraft): DdlEvent
    suspend fun setCompleted(id: String, completed: Boolean)
    suspend fun setArchived(id: String, archived: Boolean)
    suspend fun delete(id: String)
    suspend fun setUrgentMinutes(minutes: Long)
    suspend fun saveSettings(settings: AppSettings)
    suspend fun saveSource(id: String?, name: String): Category
    suspend fun setSourceActive(id: String, active: Boolean)
    suspend fun saveTag(id: String?, name: String): Category
    suspend fun setTagActive(id: String, active: Boolean)
    suspend fun applyBatch(ids: Set<String>, operation: BatchOperation)
    suspend fun archiveExpired(): Int
    suspend fun setExpiredPolicy(policy: ExpiredPolicy)
    suspend fun applyExpiredPolicy(): Int
    suspend fun referencedAttachmentPaths(): Set<String>
}
