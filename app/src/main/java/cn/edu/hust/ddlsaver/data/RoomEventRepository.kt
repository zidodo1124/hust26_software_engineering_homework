package cn.edu.hust.ddlsaver.data

import androidx.room.withTransaction
import cn.edu.hust.ddlsaver.domain.AppSettings
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.EventConflictException
import cn.edu.hust.ddlsaver.domain.EventDraft
import cn.edu.hust.ddlsaver.domain.EventNotFoundException
import cn.edu.hust.ddlsaver.domain.EventRepository
import cn.edu.hust.ddlsaver.domain.EventValidation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.util.UUID

class RoomEventRepository(
    private val database: SaverDatabase,
    private val clock: Clock = Clock.systemUTC(),
) : EventRepository {
    private val dao = database.dao()

    override val events: Flow<List<DdlEvent>> = dao.observeEvents().map { rows -> rows.map { it.toDomain() } }
    override val settings: Flow<AppSettings> = dao.observeSettings().map { row ->
        row?.let { AppSettings(it.urgentMinutes) } ?: AppSettings()
    }

    override suspend fun save(draft: EventDraft): DdlEvent {
        val normalized = EventValidation.normalize(draft)
        return database.withTransaction {
            val now = clock.millis()
            val id = normalized.id
            val row = if (id == null) {
                EventEntity(
                    id = UUID.randomUUID().toString(), title = normalized.title,
                    deadlineAt = normalized.deadlineAt, sprintMinutes = normalized.sprintMinutes,
                    description = normalized.description, importance = normalized.importance,
                    completedAt = null, archivedAt = null, createdAt = now, updatedAt = now, revision = 1,
                ).also { dao.insertEvent(it) }
            } else {
                val previous = dao.findEvent(id) ?: throw EventNotFoundException()
                if (normalized.expectedRevision != previous.revision) throw EventConflictException()
                previous.copy(
                    title = normalized.title, deadlineAt = normalized.deadlineAt,
                    sprintMinutes = normalized.sprintMinutes, description = normalized.description,
                    importance = normalized.importance, updatedAt = now,
                    revision = Math.addExact(previous.revision, 1L),
                ).also { if (dao.updateEvent(it) != 1) throw EventNotFoundException() }
            }
            row.toDomain()
        }
    }

    override suspend fun setCompleted(id: String, completed: Boolean) {
        database.withTransaction {
            val row = dao.findEvent(id) ?: throw EventNotFoundException()
            if ((row.completedAt != null) != completed) {
                val now = clock.millis()
                val updated = row.copy(
                    completedAt = if (completed) now else null,
                    updatedAt = now, revision = Math.addExact(row.revision, 1L),
                )
                if (dao.updateEvent(updated) != 1) throw EventNotFoundException()
            }
        }
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        database.withTransaction {
            val row = dao.findEvent(id) ?: throw EventNotFoundException()
            if ((row.archivedAt != null) != archived) {
                val now = clock.millis()
                val updated = row.copy(
                    archivedAt = if (archived) now else null,
                    updatedAt = now, revision = Math.addExact(row.revision, 1L),
                )
                if (dao.updateEvent(updated) != 1) throw EventNotFoundException()
            }
        }
    }

    override suspend fun delete(id: String) {
        database.withTransaction {
            if (dao.deleteEvent(id) != 1) throw EventNotFoundException()
        }
    }

    override suspend fun setUrgentMinutes(minutes: Long) {
        EventValidation.validateMinutes(minutes)
        database.withTransaction { dao.saveSettings(SettingsEntity(urgentMinutes = minutes)) }
    }
}
