package cn.edu.hust.ddlsaver.data

import androidx.room.withTransaction
import cn.edu.hust.ddlsaver.reminders.ReminderPlanStore
import cn.edu.hust.ddlsaver.domain.AppSettings
import cn.edu.hust.ddlsaver.domain.BatchOperation
import cn.edu.hust.ddlsaver.domain.Category
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.EventConflictException
import cn.edu.hust.ddlsaver.domain.EventDraft
import cn.edu.hust.ddlsaver.domain.EventNotFoundException
import cn.edu.hust.ddlsaver.domain.EventRepository
import cn.edu.hust.ddlsaver.domain.EventValidation
import cn.edu.hust.ddlsaver.domain.EventValidationException
import cn.edu.hust.ddlsaver.domain.ExpiredPolicy
import cn.edu.hust.ddlsaver.domain.RecurrenceRule
import cn.edu.hust.ddlsaver.domain.RecurrenceRules
import cn.edu.hust.ddlsaver.domain.RecurrenceFrequency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class RoomEventRepository(
    private val database: SaverDatabase,
    private val clock: Clock = Clock.systemUTC(),
) : EventRepository {
    private val dao = database.dao()

    override val events: Flow<List<DdlEvent>> = dao.observeEvents().map { rows -> rows.map { it.toDomain() } }
    override val settings: Flow<AppSettings> = dao.observeSettings().map { row ->
        row?.toDomain() ?: AppSettings()
    }
    override val sources: Flow<List<Category>> = dao.observeSources().map { rows -> rows.map { it.toDomain() } }
    override val tags: Flow<List<Category>> = dao.observeTags().map { rows -> rows.map { it.toDomain() } }

    /** Creates a durable series and materializes a caller-specified finite window atomically. */
    suspend fun createRecurrence(draft: EventDraft, rule: RecurrenceRule, through: Instant): String {
        val normalized = EventValidation.normalize(draft)
        val template = RecurrenceTemplate.encode(normalized)
        if (rule.firstDeadline.toEpochMilli() != normalized.deadlineAt || through < rule.firstDeadline) {
            throw EventValidationException("重复规则的首次截止时间或生成范围无效。")
        }
        return reminderTransaction {
            validateRelations("", normalized, null)
            val id = UUID.randomUUID().toString()
            dao.insertRecurrenceSeries(RecurrenceSeriesEntity(id, normalized.deadlineAt, rule.frequency.name,
                rule.untilDate?.toEpochDay(), rule.occurrenceCount, 1, template))
            materializeRecurrence(id, rule.firstDeadline, through)
            id
        }
    }

    /** Transaction and the unique (series, index) key make retries idempotent. */
    suspend fun extendRecurrence(id: String, from: Instant, through: Instant): Int = reminderTransaction {
        materializeRecurrence(id, from, through)
    }

    private suspend fun materializeRecurrence(id: String, from: Instant, through: Instant): Int {
        val series = dao.findRecurrenceSeries(id) ?: throw EventValidationException("重复系列已不存在。")
        val rule = RecurrenceRule(Instant.ofEpochMilli(series.firstDeadline), RecurrenceFrequency.valueOf(series.frequency),
            series.untilEpochDay?.let(LocalDate::ofEpochDay), series.occurrenceCount)
        val known = dao.recurrenceOccurrences(id).map { it.occurrenceIndex }.toSet()
        val missing = RecurrenceRules.missingBetween(rule, from, through, known)
        val now = clock.millis()
        missing.forEach { occurrence ->
            val draft = RecurrenceTemplate.decode(series.templateJson, occurrence.deadline.toEpochMilli())
            val eventId = UUID.randomUUID().toString()
            dao.insertEvent(EventEntity(id = eventId, title = draft.title, deadlineAt = draft.deadlineAt,
                sprintMinutes = draft.sprintMinutes, description = draft.description, importance = draft.importance,
                completedAt = null, archivedAt = null, createdAt = now, updatedAt = now, revision = 1, sourceId = draft.sourceId))
            dao.insertEventTags(draft.tagIds.map { EventTagEntity(eventId, it) })
            dao.insertCustomFields(draft.customFields.mapIndexed { position, field ->
                CustomFieldEntity(eventId, field.id, field.label, field.value, position)
            })
            check(dao.insertRecurrenceOccurrence(RecurrenceOccurrenceEntity(id, occurrence.index,
                occurrence.deadline.toEpochMilli(), eventId, false)) != -1L)
        }
        return missing.size
    }

    override suspend fun save(draft: EventDraft): DdlEvent {
        val normalized = EventValidation.normalize(draft)
        normalized.recurrence?.let { spec ->
            if (normalized.id != null) throw EventValidationException("不能从已有事项创建重复系列。")
            val first = Instant.ofEpochMilli(normalized.deadlineAt)
            val rule = try { RecurrenceRule(first, spec.frequency, spec.untilDate, spec.count) }
                catch (_: IllegalArgumentException) { throw EventValidationException("重复结束条件无效，请检查日期或次数。") }
            val horizon = maxOf(first, clock.instant()).plusSeconds(90L * 86_400L)
            val seriesId = createRecurrence(normalized.copy(recurrence = null), rule, horizon)
            val firstId = dao.recurrenceOccurrences(seriesId).firstOrNull()?.eventId
                ?: throw IllegalStateException("Missing first occurrence")
            return checkNotNull(dao.findEventWithRelations(firstId)).toDomain()
        }
        return reminderTransaction {
            val now = clock.millis()
            val id = normalized.id ?: UUID.randomUUID().toString()
            val previous = normalized.id?.let { dao.findEvent(it) ?: throw EventNotFoundException() }
            if (previous != null && normalized.expectedRevision != previous.revision) throw EventConflictException()
            validateRelations(id, normalized, previous)
            val row = if (previous == null) {
                EventEntity(
                    id = id, title = normalized.title, deadlineAt = normalized.deadlineAt,
                    sprintMinutes = normalized.sprintMinutes, description = normalized.description,
                    importance = normalized.importance, completedAt = null, archivedAt = null,
                    createdAt = now, updatedAt = now, revision = 1, sourceId = normalized.sourceId,
                ).also { dao.insertEvent(it) }
            } else {
                previous.copy(
                    title = normalized.title, deadlineAt = normalized.deadlineAt,
                    sprintMinutes = normalized.sprintMinutes, description = normalized.description,
                    importance = normalized.importance, sourceId = normalized.sourceId, updatedAt = now,
                    revision = Math.addExact(previous.revision, 1L),
                ).also { if (dao.updateEvent(it) != 1) throw EventNotFoundException() }
            }
            dao.clearEventTags(id)
            dao.insertEventTags(normalized.tagIds.map { EventTagEntity(id, it) })
            dao.clearCustomFields(id)
            dao.insertCustomFields(normalized.customFields.mapIndexed { position, field ->
                CustomFieldEntity(id, field.id, field.label, field.value, position)
            })
            dao.clearAttachments(id)
            dao.insertAttachments(normalized.attachments.mapIndexed { position, attachment ->
                AttachmentEntity(
                    attachment.id, id, attachment.relativePath, attachment.mimeType, attachment.displayName,
                    attachment.byteSize, attachment.width, attachment.height, position,
                )
            })
            checkNotNull(dao.findEventWithRelations(row.id)).toDomain()
        }
    }

    /** Called on app/notification reconciliation; generated records remain finite and idempotent. */
    suspend fun extendRecurrencesThrough(through: Instant) {
        reminderTransaction {
            dao.readRecurrenceSeries().forEach { series ->
                val first = Instant.ofEpochMilli(series.firstDeadline)
                if (first <= through) materializeRecurrence(series.id, first, through)
            }
        }
    }

    private suspend fun validateRelations(id: String, draft: EventDraft, previous: EventEntity?) {
        draft.sourceId?.let { sourceId ->
            val source = dao.findSource(sourceId) ?: throw EventValidationException("所选来源已不存在，请重新选择。")
            if (!source.isActive && previous?.sourceId != sourceId) {
                throw EventValidationException("该来源已停用，不能添加到新事项。")
            }
        }
        val previousTags = if (previous == null) emptySet() else dao.eventTagIds(id).toSet()
        draft.tagIds.forEach { tagId ->
            val tag = dao.findTag(tagId) ?: throw EventValidationException("所选标签已不存在，请重新选择。")
            if (!tag.isActive && tagId !in previousTags) {
                throw EventValidationException("该标签已停用，不能新增关联。")
            }
        }
        draft.attachments.forEach { attachment ->
            val owners = dao.findAttachmentOwners(attachment.id, attachment.relativePath)
            if (owners.any { it.eventId != id || it.id != attachment.id || it.relativePath != attachment.relativePath }) {
                throw EventValidationException("图片已被其他事项使用，或附件标识不一致，请重新导入。")
            }
        }
    }

    override suspend fun setCompleted(id: String, completed: Boolean) {
        reminderTransaction {
            val row = dao.findEvent(id) ?: throw EventNotFoundException()
            if ((row.completedAt != null) != completed) {
                val now = clock.millis()
                updateExisting(row.copy(
                    completedAt = if (completed) now else null,
                    updatedAt = now, revision = Math.addExact(row.revision, 1L),
                ))
            }
        }
    }

    override suspend fun setArchived(id: String, archived: Boolean) {
        reminderTransaction {
            val row = dao.findEvent(id) ?: throw EventNotFoundException()
            if ((row.archivedAt != null) != archived) {
                val now = clock.millis()
                updateExisting(row.copy(
                    archivedAt = if (archived) now else null,
                    updatedAt = now, revision = Math.addExact(row.revision, 1L),
                ))
            }
        }
    }

    override suspend fun delete(id: String) {
        reminderTransaction {
            if (dao.deleteEvent(id) != 1) throw EventNotFoundException()
            dao.skipOccurrenceForDeletedEvent(id)
        }
    }

    override suspend fun applyBatch(ids: Set<String>, operation: BatchOperation) {
        val selected = ids.toSet()
        if (selected.isEmpty()) return
        reminderTransaction {
            // Resolve the entire frozen selection before the first mutation; stale selections roll back.
            val rows = dao.findEvents(selected)
            if (rows.size != selected.size) throw EventNotFoundException()
            val now = clock.millis()
            rows.forEach { row ->
                when (operation) {
                    BatchOperation.COMPLETE -> if (row.completedAt == null) updateExisting(row.copy(
                        completedAt = now, updatedAt = now, revision = Math.addExact(row.revision, 1L),
                    ))
                    BatchOperation.ARCHIVE -> if (row.archivedAt == null) updateExisting(row.copy(
                        archivedAt = now, updatedAt = now, revision = Math.addExact(row.revision, 1L),
                    ))
                    BatchOperation.DELETE -> {
                        if (dao.deleteEvent(row.id) != 1) throw EventNotFoundException()
                        dao.skipOccurrenceForDeletedEvent(row.id)
                    }
                }
            }
        }
    }

    override suspend fun setUrgentMinutes(minutes: Long) {
        EventValidation.validateMinutes(minutes)
        reminderTransaction {
            val previous = dao.readSettings() ?: SettingsEntity(urgentMinutes = 1440)
            dao.saveSettings(previous.copy(urgentMinutes = minutes))
        }
    }

    override suspend fun setExpiredPolicy(policy: ExpiredPolicy) {
        reminderTransaction {
            val previous = dao.readSettings() ?: SettingsEntity(urgentMinutes = 1440)
            dao.saveSettings(previous.copy(expiredPolicy = policy))
        }
    }

    override suspend fun saveSettings(settings: AppSettings) {
        EventValidation.validateSettings(settings)
        reminderTransaction {
            dao.saveSettings(settings.toEntity())
        }
    }

    override suspend fun archiveExpired(): Int = reminderTransaction {
        archiveExpiredRows(includeCompleted = true)
    }

    override suspend fun applyExpiredPolicy(): Int = reminderTransaction {
        if (dao.readSettings()?.expiredPolicy != ExpiredPolicy.AUTO_ARCHIVE) 0
        else archiveExpiredRows(includeCompleted = false)
    }

    private suspend fun archiveExpiredRows(includeCompleted: Boolean): Int {
        val now = clock.millis()
        val rows = dao.findExpired(now, includeCompleted)
        rows.forEach { row ->
            updateExisting(row.copy(archivedAt = now, updatedAt = now, revision = Math.addExact(row.revision, 1L)))
        }
        return rows.size
    }

    private suspend fun updateExisting(row: EventEntity) {
        if (dao.updateEvent(row) != 1) throw EventNotFoundException()
    }

    override suspend fun saveSource(id: String?, name: String): Category {
        val normalized = EventValidation.normalizeCategoryName(name)
        return reminderTransaction {
            if (dao.readSources().any { it.id != id && it.name.equals(normalized, ignoreCase = true) }) {
                throw EventValidationException("已有同名来源，请使用或恢复原来源。")
            }
            val row = if (id == null) SourceEntity(UUID.randomUUID().toString(), normalized)
            else (dao.findSource(id) ?: throw EventValidationException("来源已不存在。")).copy(name = normalized)
            if (id == null) dao.insertSource(row) else if (dao.updateSource(row) != 1) {
                throw EventValidationException("来源已不存在。")
            }
            row.toDomain()
        }
    }

    override suspend fun setSourceActive(id: String, active: Boolean) {
        reminderTransaction {
            val row = dao.findSource(id) ?: throw EventValidationException("来源已不存在。")
            if (dao.updateSource(row.copy(isActive = active)) != 1) throw EventValidationException("来源已不存在。")
        }
    }

    override suspend fun saveTag(id: String?, name: String): Category {
        val normalized = EventValidation.normalizeCategoryName(name)
        return reminderTransaction {
            if (dao.readTags().any { it.id != id && it.name.equals(normalized, ignoreCase = true) }) {
                throw EventValidationException("已有同名标签，请使用或恢复原标签。")
            }
            val row = if (id == null) TagEntity(UUID.randomUUID().toString(), normalized)
            else (dao.findTag(id) ?: throw EventValidationException("标签已不存在。")).copy(name = normalized)
            if (id == null) dao.insertTag(row) else if (dao.updateTag(row) != 1) {
                throw EventValidationException("标签已不存在。")
            }
            row.toDomain()
        }
    }

    override suspend fun setTagActive(id: String, active: Boolean) {
        reminderTransaction {
            val row = dao.findTag(id) ?: throw EventValidationException("标签已不存在。")
            if (dao.updateTag(row.copy(isActive = active)) != 1) throw EventValidationException("标签已不存在。")
        }
    }

    override suspend fun referencedAttachmentPaths(): Set<String> = dao.referencedAttachmentPaths().toSet()

    private suspend fun <T> reminderTransaction(action: suspend () -> T): T = database.withTransaction {
        val result = action()
        ReminderPlanStore.rebuild(dao, clock.millis())
        result
    }
}
