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
import cn.edu.hust.ddlsaver.domain.UrgencyRules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
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
            throw EventValidationException("首次截止时间或生成日期范围不对，请检查重复设置。")
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
        val series = dao.findRecurrenceSeries(id) ?: throw EventValidationException("这组重复事项已经不在了。")
        if (series.occurrenceCount == 0) return 0
        val rule = RecurrenceRule(Instant.ofEpochMilli(series.firstDeadline), RecurrenceFrequency.valueOf(series.frequency),
            series.untilEpochDay?.let(LocalDate::ofEpochDay), series.occurrenceCount, series.monthlyAnchorDay)
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
        normalized.recurrence?.takeUnless { normalized.applyToFuture }?.let { spec ->
            val first = Instant.ofEpochMilli(normalized.deadlineAt)
            val rule = try { RecurrenceRule(first, spec.frequency, spec.untilDate, spec.count) }
                catch (_: IllegalArgumentException) { throw EventValidationException("重复的结束日期或次数不对，请检查一下。") }
            val horizon = maxOf(first, clock.instant()).plusSeconds(90L * 86_400L)
            if (normalized.id != null) return convertToRecurrence(normalized, rule, horizon)
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
            if (normalized.stopAfterThis) return@reminderTransaction stopSeriesAfterOccurrence(normalized, previous)
            if (normalized.applyToFuture) return@reminderTransaction updateSeriesFromOccurrence(normalized, previous)
            if (previous != null && normalized.attachments.isNotEmpty() && dao.recurrenceForEvent(previous.id) != null) {
                throw EventValidationException("重复事项还不能带图片，请先移除图片。")
            }
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

    private suspend fun stopSeriesAfterOccurrence(draft: EventDraft, previous: EventEntity?): DdlEvent {
        val current = previous ?: throw EventValidationException("请先选择一条重复事项。")
        val occurrence = dao.recurrenceForEvent(current.id) ?: throw EventValidationException("这条事项没有设置重复。")
        if (!draft.applyToFuture || current.completedAt != null || current.archivedAt != null) {
            throw EventValidationException("请选择未完成且未归档的重复事项，并选择“本次及后续”。")
        }
        val series = dao.findRecurrenceSeries(occurrence.seriesId) ?: throw EventValidationException("这组重复事项已经不在了。")
        val limit = try { Math.toIntExact(Math.addExact(occurrence.occurrenceIndex, 1L)) } catch (_: ArithmeticException) {
            throw EventValidationException("重复次数超出了支持范围。")
        }
        val saved = save(draft.copy(recurrence = null, applyToFuture = false, stopAfterThis = false))
        if (dao.updateRecurrenceSeries(series.copy(occurrenceCount = minOf(series.occurrenceCount ?: limit, limit),
                untilEpochDay = null, revision = Math.addExact(series.revision, 1L))) != 1) {
            throw EventValidationException("这组重复事项已经不在了。")
        }
        dao.recurrenceOccurrences(series.id).filter { it.occurrenceIndex > occurrence.occurrenceIndex }.forEach { item ->
            val row = item.eventId?.let { dao.findEvent(it) } ?: return@forEach
            if (row.completedAt == null && row.archivedAt == null) {
                if (dao.deleteEvent(row.id) != 1) throw EventNotFoundException()
                dao.skipOccurrenceForDeletedEvent(row.id)
            }
        }
        return checkNotNull(dao.findEventWithRelations(saved.id)).toDomain()
    }

    private suspend fun convertToRecurrence(draft: EventDraft, rule: RecurrenceRule, through: Instant): DdlEvent {
        val template = RecurrenceTemplate.encode(draft.copy(id = null, expectedRevision = null, recurrence = null))
        return reminderTransaction {
            val id = checkNotNull(draft.id)
            if (dao.recurrenceForEvent(id) != null) throw EventValidationException("这条事项已经设置过重复了。")
            val existing = save(draft.copy(recurrence = null))
            val seriesId = UUID.randomUUID().toString()
            dao.insertRecurrenceSeries(RecurrenceSeriesEntity(seriesId, existing.deadlineAt, rule.frequency.name,
                rule.untilDate?.toEpochDay(), rule.occurrenceCount, 1, template))
            check(dao.insertRecurrenceOccurrence(RecurrenceOccurrenceEntity(seriesId, 0, existing.deadlineAt, existing.id, false)) != -1L)
            materializeRecurrence(seriesId, rule.firstDeadline, through)
            checkNotNull(dao.findEventWithRelations(id)).toDomain()
        }
    }

    private suspend fun updateSeriesFromOccurrence(draft: EventDraft, previous: EventEntity?): DdlEvent {
        val current = previous ?: throw EventValidationException("请先选择一条重复事项。")
        val occurrence = dao.recurrenceForEvent(current.id) ?: throw EventValidationException("这条事项没有设置重复。")
        if (current.completedAt != null || current.archivedAt != null) throw EventValidationException("这条事项已完成或归档，只能修改本次。")
        if (draft.attachments.isNotEmpty()) throw EventValidationException("重复事项还不能带图片，请先移除图片。")
        validateRelations(current.id, draft, current)
        val series = dao.findRecurrenceSeries(occurrence.seriesId) ?: throw EventValidationException("这组重复事项已经不在了。")
        val oldFrequency = RecurrenceFrequency.valueOf(series.frequency)
        val frequency = draft.recurrence?.frequency ?: oldFrequency
        val untilDate = if (draft.recurrence != null) draft.recurrence.untilDate else series.untilEpochDay?.let(LocalDate::ofEpochDay)
        val count = if (draft.recurrence != null) draft.recurrence.count else series.occurrenceCount
        val selected = Instant.ofEpochMilli(draft.deadlineAt).atZone(UrgencyRules.BEIJING_ZONE)
        val original = Instant.ofEpochMilli(occurrence.scheduledDeadline).atZone(UrgencyRules.BEIJING_ZONE)
        val anchorDay = if (frequency != RecurrenceFrequency.MONTHLY) null else if (frequency == oldFrequency && selected.toLocalDate() == original.toLocalDate()) {
            series.monthlyAnchorDay ?: Instant.ofEpochMilli(series.firstDeadline).atZone(UrgencyRules.BEIJING_ZONE).dayOfMonth
        } else selected.dayOfMonth
        val firstDeadline = if (frequency == oldFrequency && draft.deadlineAt == occurrence.scheduledDeadline) series.firstDeadline else {
            val first = when (frequency) {
                RecurrenceFrequency.DAILY -> selected.minusDays(occurrence.occurrenceIndex)
                RecurrenceFrequency.WEEKLY -> selected.minusWeeks(occurrence.occurrenceIndex)
                RecurrenceFrequency.MONTHLY -> {
                    val month = YearMonth.from(selected).minusMonths(occurrence.occurrenceIndex)
                    month.atDay(minOf(checkNotNull(anchorDay), month.lengthOfMonth())).atTime(selected.toLocalTime())
                        .atZone(UrgencyRules.BEIJING_ZONE)
                }
            }
            first.toInstant().toEpochMilli()
        }
        val updatedRule = try { RecurrenceRule(Instant.ofEpochMilli(firstDeadline), frequency, untilDate, count, anchorDay) }
            catch (_: IllegalArgumentException) { throw EventValidationException("新的截止日期晚于重复结束日期，请调整日期。") }
        if (RecurrenceRules.occurrenceAt(updatedRule, occurrence.occurrenceIndex)?.deadline?.toEpochMilli() != draft.deadlineAt) {
            throw EventValidationException("这个日期超出了结束范围，或与按月重复的规则对不上。可以选择“仅本次”修改。")
        }
        val template = RecurrenceTemplate.encode(draft.copy(id = null, expectedRevision = null, recurrence = null, applyToFuture = false))
        if (dao.updateRecurrenceSeries(series.copy(firstDeadline = firstDeadline, frequency = frequency.name,
                untilEpochDay = untilDate?.toEpochDay(), occurrenceCount = count, templateJson = template,
                monthlyAnchorDay = anchorDay, revision = Math.addExact(series.revision, 1L))) != 1) {
            throw EventValidationException("这组重复事项已经不在了。")
        }
        val now = clock.millis()
        dao.recurrenceOccurrences(series.id).filter { it.occurrenceIndex >= occurrence.occurrenceIndex }.forEach { item ->
            val row = item.eventId?.let { dao.findEvent(it) } ?: return@forEach
            if (row.completedAt != null || row.archivedAt != null) return@forEach
            val nextDeadline = RecurrenceRules.occurrenceAt(updatedRule, item.occurrenceIndex)?.deadline?.toEpochMilli()
            if (nextDeadline == null) {
                if (dao.deleteEvent(row.id) != 1) throw EventNotFoundException()
                check(dao.deleteRecurrenceOccurrence(series.id, item.occurrenceIndex) == 1)
                return@forEach
            }
            updateExisting(row.copy(title = draft.title, deadlineAt = nextDeadline, sprintMinutes = draft.sprintMinutes, description = draft.description,
                importance = draft.importance, sourceId = draft.sourceId, updatedAt = now, revision = Math.addExact(row.revision, 1L)))
            check(dao.updateRecurrenceOccurrence(item.copy(scheduledDeadline = nextDeadline)) == 1)
            dao.clearEventTags(row.id)
            dao.insertEventTags(draft.tagIds.map { EventTagEntity(row.id, it) })
            dao.clearCustomFields(row.id)
            dao.insertCustomFields(draft.customFields.mapIndexed { index, field ->
                CustomFieldEntity(row.id, field.id, field.label, field.value, index)
            })
        }
        return checkNotNull(dao.findEventWithRelations(current.id)).toDomain()
    }

    /** Stops this and future periods, preserving completed or archived history. */
    suspend fun deleteRecurrenceFollowing(eventId: String) {
        reminderTransaction {
            val event = dao.findEvent(eventId) ?: throw EventNotFoundException()
            val occurrence = dao.recurrenceForEvent(eventId) ?: throw EventValidationException("这条事项没有设置重复。")
            if (event.completedAt != null || event.archivedAt != null) throw EventValidationException("这条事项已完成或归档，只能删除本次。")
            val series = dao.findRecurrenceSeries(occurrence.seriesId) ?: throw EventValidationException("这组重复事项已经不在了。")
            val limit = try { Math.toIntExact(occurrence.occurrenceIndex) } catch (_: ArithmeticException) {
                throw EventValidationException("重复次数超出了支持范围。")
            }
            if (dao.updateRecurrenceSeries(series.copy(occurrenceCount = minOf(series.occurrenceCount ?: limit, limit),
                    untilEpochDay = null, revision = Math.addExact(series.revision, 1L))) != 1) {
                throw EventValidationException("这组重复事项已经不在了。")
            }
            dao.recurrenceOccurrences(series.id).filter { it.occurrenceIndex >= occurrence.occurrenceIndex }.forEach { item ->
                val row = item.eventId?.let { dao.findEvent(it) } ?: return@forEach
                if (row.completedAt == null && row.archivedAt == null) {
                    if (dao.deleteEvent(row.id) != 1) throw EventNotFoundException()
                    dao.skipOccurrenceForDeletedEvent(row.id)
                }
            }
        }
    }

    /** Called on app/notification reconciliation; generated records remain finite and idempotent. */
    suspend fun extendRecurrencesThrough(through: Instant) {
        reminderTransaction {
            dao.readRecurrenceSeries().forEach { series ->
                if (series.occurrenceCount == 0) return@forEach
                val rule = RecurrenceRule(Instant.ofEpochMilli(series.firstDeadline), RecurrenceFrequency.valueOf(series.frequency),
                    series.untilEpochDay?.let(LocalDate::ofEpochDay), series.occurrenceCount, series.monthlyAnchorDay)
                val known = dao.recurrenceOccurrences(series.id).map { it.occurrenceIndex }.toHashSet()
                var firstMissing = 0L
                while (firstMissing in known) firstMissing++
                val from = RecurrenceRules.occurrenceAt(rule, firstMissing)?.deadline ?: return@forEach
                if (from <= through) materializeRecurrence(series.id, from, through)
            }
        }
    }

    private suspend fun validateRelations(id: String, draft: EventDraft, previous: EventEntity?) {
        draft.sourceId?.let { sourceId ->
            val source = dao.findSource(sourceId) ?: throw EventValidationException("选的来源已经不在了，请重新选择。")
            if (!source.isActive && previous?.sourceId != sourceId) {
                throw EventValidationException("这个来源已停用，不能再添加到事项里。")
            }
        }
        val previousTags = if (previous == null) emptySet() else dao.eventTagIds(id).toSet()
        draft.tagIds.forEach { tagId ->
            val tag = dao.findTag(tagId) ?: throw EventValidationException("选的标签已经不在了，请重新选择。")
            if (!tag.isActive && tagId !in previousTags) {
                throw EventValidationException("这个标签已停用，不能再添加到事项里。")
            }
        }
        draft.attachments.forEach { attachment ->
            val owners = dao.findAttachmentOwners(attachment.id, attachment.relativePath)
            if (owners.any { it.eventId != id || it.id != attachment.id || it.relativePath != attachment.relativePath }) {
                throw EventValidationException("这张图片已被其他事项使用，或图片信息不一致，请重新添加。")
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
                throw EventValidationException("已经有同名来源了，请选用已有来源，或先恢复停用的来源。")
            }
            val row = if (id == null) SourceEntity(UUID.randomUUID().toString(), normalized)
            else (dao.findSource(id) ?: throw EventValidationException("这个来源已经不在了。")).copy(name = normalized)
            if (id == null) dao.insertSource(row) else if (dao.updateSource(row) != 1) {
                throw EventValidationException("这个来源已经不在了。")
            }
            row.toDomain()
        }
    }

    override suspend fun setSourceActive(id: String, active: Boolean) {
        reminderTransaction {
            val row = dao.findSource(id) ?: throw EventValidationException("这个来源已经不在了。")
            if (dao.updateSource(row.copy(isActive = active)) != 1) throw EventValidationException("这个来源已经不在了。")
        }
    }

    override suspend fun saveTag(id: String?, name: String): Category {
        val normalized = EventValidation.normalizeCategoryName(name)
        return reminderTransaction {
            if (dao.readTags().any { it.id != id && it.name.equals(normalized, ignoreCase = true) }) {
                throw EventValidationException("已经有同名标签了，请选用已有标签，或先恢复停用的标签。")
            }
            val row = if (id == null) TagEntity(UUID.randomUUID().toString(), normalized)
            else (dao.findTag(id) ?: throw EventValidationException("这个标签已经不在了。")).copy(name = normalized)
            if (id == null) dao.insertTag(row) else if (dao.updateTag(row) != 1) {
                throw EventValidationException("这个标签已经不在了。")
            }
            row.toDomain()
        }
    }

    override suspend fun setTagActive(id: String, active: Boolean) {
        reminderTransaction {
            val row = dao.findTag(id) ?: throw EventValidationException("这个标签已经不在了。")
            if (dao.updateTag(row.copy(isActive = active)) != 1) throw EventValidationException("这个标签已经不在了。")
        }
    }

    override suspend fun referencedAttachmentPaths(): Set<String> = dao.referencedAttachmentPaths().toSet()

    private suspend fun <T> reminderTransaction(action: suspend () -> T): T = database.withTransaction {
        val result = action()
        ReminderPlanStore.rebuild(dao, clock.millis())
        result
    }
}
