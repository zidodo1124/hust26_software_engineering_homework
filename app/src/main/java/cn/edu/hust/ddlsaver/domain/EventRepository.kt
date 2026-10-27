package cn.edu.hust.ddlsaver.domain

import kotlinx.coroutines.flow.Flow

interface EventRepository {
    val events: Flow<List<DdlEvent>>
    val settings: Flow<AppSettings>

    /** New drafts have no ID; edits must carry the revision originally opened by the editor. */
    suspend fun save(draft: EventDraft): DdlEvent
    suspend fun setCompleted(id: String, completed: Boolean)
    suspend fun setArchived(id: String, archived: Boolean)
    suspend fun delete(id: String)
    suspend fun setUrgentMinutes(minutes: Long)
}
