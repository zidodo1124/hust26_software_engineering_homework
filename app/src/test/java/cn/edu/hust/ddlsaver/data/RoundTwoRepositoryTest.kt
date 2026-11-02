package cn.edu.hust.ddlsaver.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cn.edu.hust.ddlsaver.domain.AppSettings
import cn.edu.hust.ddlsaver.domain.AttachmentRef
import cn.edu.hust.ddlsaver.domain.BatchOperation
import cn.edu.hust.ddlsaver.domain.CustomField
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.EventConflictException
import cn.edu.hust.ddlsaver.domain.EventDraft
import cn.edu.hust.ddlsaver.domain.EventFilter
import cn.edu.hust.ddlsaver.domain.EventNotFoundException
import cn.edu.hust.ddlsaver.domain.EventQuery
import cn.edu.hust.ddlsaver.domain.EventValidationException
import cn.edu.hust.ddlsaver.domain.ExpiredPolicy
import cn.edu.hust.ddlsaver.domain.RecurrenceFrequency
import cn.edu.hust.ddlsaver.domain.RecurrenceRule
import cn.edu.hust.ddlsaver.domain.RecurrenceSpec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RoundTwoRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: SaverDatabase
    private lateinit var repository: RoomEventRepository
    private lateinit var databaseName: String
    private val clock = MutableClock(Instant.parse("2030-01-02T00:00:00Z"))

    @Before
    fun openDatabase() {
        context = ApplicationProvider.getApplicationContext()
        databaseName = "round-two-${UUID.randomUUID()}.db"
        reopenDatabase()
    }

    private fun reopenDatabase() {
        database = Room.databaseBuilder(context, SaverDatabase::class.java, databaseName)
            .addMigrations(SaverDatabase.MIGRATION_1_2).build()
        repository = RoomEventRepository(database, clock)
    }

    @After
    fun closeDatabase() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    private fun draft(title: String = "报告") = EventDraft(title = title, deadlineAt = clock.millis() + 3_600_000)

    private fun DdlEvent.edit() = EventDraft(
        id = id, title = title, deadlineAt = deadlineAt, sprintMinutes = sprintMinutes,
        description = description, importance = importance, expectedRevision = revision,
        sourceId = source?.id, tagIds = tags.map { it.id }.toSet(), customFields = customFields,
        attachments = attachments,
    )

    private fun attachment(name: String = "课程截图.png"): AttachmentRef {
        val id = UUID.randomUUID().toString()
        return AttachmentRef(id, "$id.img", "image/png", name, 2048, 120, 80)
    }

    @Test
    fun recurrenceTemplateSurvivesReopenAndGenerationIsIdempotent() = runBlocking {
        val start = clock.instant().plusSeconds(3600)
        val source = repository.saveSource(null, "课程")
        val tag = repository.saveTag(null, "作业")
        val template = EventDraft(title = "周期作业", deadlineAt = start.toEpochMilli(), sourceId = source.id,
            tagIds = setOf(tag.id), customFields = listOf(CustomField("place", "地点", "东九")))
        val id = repository.createRecurrence(template, RecurrenceRule(start, RecurrenceFrequency.DAILY, occurrenceCount = 3),
            start.plusSeconds(86_400))
        val initial = repository.events.first()
        assertEquals(2, initial.size)
        assertTrue(initial.all { it.source?.id == source.id && it.tags.single().id == tag.id && it.customFields.single().value == "东九" })
        repository.setCompleted(initial.first().id, true)
        repository.delete(initial.last().id)
        repository.setSourceActive(source.id, false)
        repository.setTagActive(tag.id, false)
        database.close()
        reopenDatabase()
        assertEquals(1, repository.extendRecurrence(id, start, start.plusSeconds(2 * 86_400)))
        assertEquals(0, repository.extendRecurrence(id, start, start.plusSeconds(2 * 86_400)))
        val rows = repository.events.first()
        assertEquals(2, rows.size)
        assertNotNull(rows.first { it.id == initial.first().id }.completedAt)
        assertTrue(database.dao().recurrenceOccurrences(id)[1].skipped)
        assertTrue(database.dao().readReminderPlans().any { it.eventId == rows.last().id && it.active })
    }

    @Test
    fun savingRepeatDraftAndExtendingWindowPreservesIndependentStates() = runBlocking {
        val first = repository.save(draft("每周作业").copy(
            recurrence = RecurrenceSpec(RecurrenceFrequency.WEEKLY, count = 20)))
        assertNotNull(first.recurrence)
        val initially = repository.events.first()
        assertTrue(initially.size in 12..14)
        assertTrue(runCatching { repository.save(first.edit().copy(attachments = listOf(attachment()))) }
            .exceptionOrNull() is EventValidationException)
        assertTrue(repository.events.first().first { it.id == first.id }.attachments.isEmpty())
        repository.setCompleted(first.id, true)
        repository.extendRecurrencesThrough(clock.instant().plusSeconds(150L * 86_400L))
        val extended = repository.events.first()
        assertEquals(20, extended.size)
        assertEquals(1, extended.count { it.completedAt != null })
        repository.extendRecurrencesThrough(clock.instant().plusSeconds(150L * 86_400L))
        assertEquals(20, repository.events.first().size)
    }

    @Test
    fun deletingOneOccurrencePreservesOtherPeriodsAndInvalidatesItsReminder() = runBlocking {
        val first = repository.save(draft("第一个周期"))
        val second = repository.save(draft("第二个周期").copy(deadlineAt = clock.millis() + 86_400_000))
        val dao = database.dao()
        dao.insertRecurrenceSeries(RecurrenceSeriesEntity("series", first.deadlineAt, "DAILY", null, 2, 1))
        dao.insertRecurrenceOccurrence(RecurrenceOccurrenceEntity("series", 0, first.deadlineAt, first.id, false))
        dao.insertRecurrenceOccurrence(RecurrenceOccurrenceEntity("series", 1, second.deadlineAt, second.id, false))
        assertTrue(dao.readReminderPlans().any { it.eventId == first.id && it.active })

        // A stale batch must leave both the event and its occurrence unchanged.
        assertTrue(runCatching { repository.applyBatch(setOf(first.id, "missing"), BatchOperation.DELETE) }
            .exceptionOrNull() is EventNotFoundException)
        assertEquals(first.id, dao.recurrenceOccurrences("series").first().eventId)
        assertEquals(2, repository.events.first().size)

        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER fail_occurrence_skip BEFORE UPDATE ON recurrence_occurrences BEGIN SELECT RAISE(ABORT, 'skip failed'); END")
        assertTrue(runCatching { repository.delete(first.id) }.isFailure)
        assertEquals(first.id, dao.recurrenceOccurrences("series").first().eventId)
        assertNotNull(dao.findEvent(first.id))
        assertTrue(dao.readReminderPlans().any { it.eventId == first.id && it.active })
        database.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_occurrence_skip")

        repository.delete(first.id)
        val occurrences = dao.recurrenceOccurrences("series")
        assertTrue(occurrences[0].skipped)
        assertNull(occurrences[0].eventId)
        assertEquals(second.id, occurrences[1].eventId)
        assertFalse(occurrences[1].skipped)
        assertEquals(listOf(second.id), repository.events.first().map { it.id })
        assertFalse(dao.readReminderPlans().any { it.eventId == first.id && it.active })
        database.close()
        reopenDatabase()
        assertEquals(occurrences, database.dao().recurrenceOccurrences("series"))

        repository.applyBatch(setOf(second.id), BatchOperation.DELETE)
        assertTrue(database.dao().recurrenceOccurrences("series").all { it.skipped && it.eventId == null })
    }

    @Test
    fun completeV2EventCatalogsAndSettingsSurviveReopening() = runBlocking {
        val source = repository.saveSource(null, " 课程群 ")
        val tagA = repository.saveTag(null, "必修")
        val tagB = repository.saveTag(null, "小组")
        val images = listOf(attachment("first.png"), attachment("second.png"))
        val fields = listOf(CustomField("location", "地点", "东九"), CustomField("url", "网址", "https://example.org"))
        val saved = repository.save(draft().copy(sourceId = source.id, tagIds = setOf(tagB.id, tagA.id),
            customFields = fields, attachments = images))
        repository.saveSettings(AppSettings(720, ExpiredPolicy.AUTO_ARCHIVE))
        database.close()
        reopenDatabase()
        assertEquals(saved, repository.events.first().single())
        assertEquals(fields, repository.events.first().single().customFields)
        assertEquals(images, repository.events.first().single().attachments)
        assertEquals(listOf(source), repository.sources.first())
        assertEquals(setOf(tagA, tagB), repository.tags.first().toSet())
        assertEquals(AppSettings(720, ExpiredPolicy.AUTO_ARCHIVE), repository.settings.first())
        assertEquals(images.map { it.relativePath }.toSet(), repository.referencedAttachmentPaths())
    }

    @Test
    fun customFieldsBelongToEachEventEvenWhenTheirIdsMatch() = runBlocking {
        val first = repository.save(draft("甲").copy(customFields = listOf(CustomField("place", "地点", "东九"))))
        val second = repository.save(draft("乙").copy(customFields = listOf(CustomField("place", "地点", "西十二"))))
        repository.save(first.edit().copy(customFields = listOf(CustomField("place", "地点", "线上"))))
        val rows = repository.events.first().associateBy { it.id }
        assertEquals("线上", rows.getValue(first.id).customFields.single().value)
        assertEquals(second, rows.getValue(second.id))
        repository.delete(first.id)
        assertEquals(second, repository.events.first().single())
    }

    @Test
    fun sameNamedFieldsAndEmptyContentRemainIndependentAcrossEditsAndReopen() = runBlocking {
        val fields = listOf(
            CustomField("first", "联系人", "甲"),
            CustomField("second", "联系人", ""),
            CustomField("note", "备注", "保留内容"),
        )
        val first = repository.save(draft("甲事项").copy(customFields = fields))
        val second = repository.save(draft("乙事项").copy(
            customFields = listOf(CustomField("second", "联系人", "乙")),
        ))
        database.close()
        reopenDatabase()
        val reopened = repository.events.first().associateBy { it.id }
        assertEquals(fields, reopened.getValue(first.id).customFields)
        assertEquals(second, reopened.getValue(second.id))

        val editedFields = listOf(fields[0], fields[1].copy(value = "丙"), fields[2])
        val edited = repository.save(reopened.getValue(first.id).edit().copy(customFields = editedFields))
        assertEquals(editedFields, edited.customFields)
        val remaining = listOf(editedFields[1], editedFields[2])
        repository.save(edited.edit().copy(customFields = remaining))
        database.close()
        reopenDatabase()
        val finalRows = repository.events.first().associateBy { it.id }
        assertEquals(remaining, finalRows.getValue(first.id).customFields)
        assertEquals(second, finalRows.getValue(second.id))
    }

    @Test
    fun renamedAndDisabledCategoriesRemainLinkedButCannotBeNewlyAssigned() = runBlocking {
        val source = repository.saveSource(null, "课程群")
        val tag = repository.saveTag(null, "作业")
        val saved = repository.save(draft().copy(sourceId = source.id, tagIds = setOf(tag.id)))
        repository.saveSource(source.id, " 软件工程群 ")
        repository.saveTag(tag.id, "大作业")
        repository.setSourceActive(source.id, false)
        repository.setTagActive(tag.id, false)
        val linked = repository.events.first().single()
        assertEquals("软件工程群", linked.source?.name)
        assertEquals("大作业", linked.tags.single().name)
        assertFalse(linked.source!!.isActive)
        assertFalse(linked.tags.single().isActive)
        assertEquals(linked.source, repository.sources.first().single())
        assertEquals(linked.tags, repository.tags.first())
        val edited = repository.save(linked.edit().copy(title = "仍保留原分类"))
        assertEquals(saved.id, edited.id)
        assertTrue(runCatching { repository.save(draft().copy(sourceId = source.id)) }
            .exceptionOrNull() is EventValidationException)
        assertTrue(runCatching { repository.save(draft().copy(tagIds = setOf(tag.id))) }
            .exceptionOrNull() is EventValidationException)
        val unrelated = repository.save(draft("无分类"))
        assertTrue(runCatching { repository.save(unrelated.edit().copy(sourceId = source.id)) }
            .exceptionOrNull() is EventValidationException)
        repository.setSourceActive(source.id, true)
        repository.setTagActive(tag.id, true)
        assertNotNull(repository.save(draft("恢复后可选").copy(sourceId = source.id, tagIds = setOf(tag.id))).source)
    }

    @Test
    fun namesAreTrimmedAndDuplicateNamesCannotBypassInactiveCatalogItems() = runBlocking {
        val source = repository.saveSource(null, " Email ")
        val tag = repository.saveTag(null, " Project ")
        repository.setSourceActive(source.id, false)
        repository.setTagActive(tag.id, false)
        assertTrue(runCatching { repository.saveSource(null, " email ") }.exceptionOrNull() is EventValidationException)
        assertTrue(runCatching { repository.saveTag(null, "PROJECT") }.exceptionOrNull() is EventValidationException)
        assertTrue(runCatching { repository.saveTag(null, " ") }.exceptionOrNull() is EventValidationException)
        assertEquals("Email", repository.sources.first().single().name)
        assertEquals("Project", repository.tags.first().single().name)
    }

    @Test
    fun disabledSameNamedCategoriesKeepOldRelationsAcrossReopenAndCanBeRestored() = runBlocking {
        val source = repository.saveSource(null, " Study ")
        val tag = repository.saveTag(null, "study")
        val otherTag = repository.saveTag(null, "课程")
        val matching = repository.save(draft("匹配项").copy(
            sourceId = source.id, tagIds = setOf(tag.id, otherTag.id),
        ))
        val partial = repository.save(draft("只含一个标签").copy(
            sourceId = source.id, tagIds = setOf(otherTag.id),
        ))
        repository.setSourceActive(source.id, false)
        repository.setTagActive(tag.id, false)
        database.close()
        reopenDatabase()

        val rows = repository.events.first().associateBy { it.id }
        val linked = rows.getValue(matching.id)
        assertFalse(linked.source!!.isActive)
        assertFalse(linked.tags.first { it.id == tag.id }.isActive)
        assertEquals(partial.copy(source = partial.source!!.copy(isActive = false)), rows.getValue(partial.id))
        val filter = EventFilter(sourceId = source.id, tagIds = setOf(tag.id, otherTag.id))
        assertEquals(listOf(matching.id), EventQuery.select(rows.values.toList(), filter, clock.instant()).map { it.id })

        val removed = repository.save(linked.edit().copy(sourceId = null, tagIds = setOf(otherTag.id)))
        assertTrue(runCatching {
            repository.save(removed.edit().copy(sourceId = source.id, tagIds = setOf(tag.id, otherTag.id)))
        }.exceptionOrNull() is EventValidationException)
        assertEquals(removed, repository.events.first().first { it.id == matching.id })
        repository.setSourceActive(source.id, true)
        repository.setTagActive(tag.id, true)
        val restored = repository.save(removed.edit().copy(sourceId = source.id, tagIds = setOf(tag.id, otherTag.id)))
        assertTrue(restored.source!!.isActive)
        assertTrue(restored.tags.first { it.id == tag.id }.isActive)
        database.close()
        reopenDatabase()
        assertEquals(restored, repository.events.first().first { it.id == matching.id })
        assertEquals(partial, repository.events.first().first { it.id == partial.id })
    }

    @Test
    fun failureDuringFieldInsertRollsBackEventRelationsAndReminderPlans() = runBlocking {
        val tag = repository.saveTag(null, "课程")
        val original = repository.save(draft("原事项").copy(
            tagIds = setOf(tag.id), customFields = listOf(CustomField("old", "地点", "东九")),
        ))
        val plansBefore = database.dao().readReminderPlans()
        database.openHelper.writableDatabase.execSQL(
            """CREATE TRIGGER reject_test_field BEFORE INSERT ON custom_fields
               WHEN NEW.label = '触发失败'
               BEGIN SELECT RAISE(ABORT, 'simulated insert failure'); END""".trimIndent()
        )
        val failure = runCatching {
            repository.save(original.edit().copy(
                title = "不应保存", tagIds = emptySet(),
                customFields = listOf(CustomField("new", "触发失败", "内容")),
            ))
        }
        assertTrue(failure.isFailure)
        assertEquals(original, repository.events.first().single())
        assertEquals(plansBefore, database.dao().readReminderPlans())
        database.close()
        reopenDatabase()
        assertEquals(original, repository.events.first().single())
        assertEquals(plansBefore, database.dao().readReminderPlans())
    }

    @Test
    fun batchUsesOnlySelectedIdsPreservesIndependentFlagsAndDeletesTheirRelations() = runBlocking {
        val selected = repository.save(draft("选择项").copy(attachments = listOf(attachment()),
            customFields = listOf(CustomField("note", "备注", "说明"))))
        val untouched = repository.save(draft("未选择"))
        repository.applyBatch(setOf(selected.id), BatchOperation.COMPLETE)
        val completed = repository.events.first().first { it.id == selected.id }
        repository.applyBatch(setOf(selected.id), BatchOperation.ARCHIVE)
        val archived = repository.events.first().first { it.id == selected.id }
        assertEquals(completed.completedAt, archived.completedAt)
        assertNotNull(archived.archivedAt)
        assertEquals(selected.revision + 2, archived.revision)
        assertEquals(untouched, repository.events.first().first { it.id == untouched.id })
        repository.applyBatch(setOf(selected.id), BatchOperation.DELETE)
        assertEquals(listOf(untouched), repository.events.first())
        assertTrue(repository.referencedAttachmentPaths().isEmpty())
        assertNull(database.dao().findEventWithRelations(selected.id))
    }

    @Test
    fun missingBatchMemberRejectsEveryOperationWithoutPartialChanges() = runBlocking {
        val saved = repository.save(draft().copy(attachments = listOf(attachment())))
        BatchOperation.entries.forEach { operation ->
            val failure = runCatching { repository.applyBatch(setOf(saved.id, "deleted-elsewhere"), operation) }
            assertTrue(failure.exceptionOrNull() is EventNotFoundException)
            assertEquals(listOf(saved), repository.events.first())
            assertEquals(saved.attachments.map { it.relativePath }.toSet(), repository.referencedAttachmentPaths())
        }
    }

    @Test
    fun failureInsideBatchTransactionRollsBackEarlierMutations() = runBlocking {
        val first = repository.save(draft("first"))
        val overflow = repository.save(draft("overflow"))
        val overflowRow = database.dao().findEvent(overflow.id)!!
        database.dao().updateEvent(overflowRow.copy(revision = Long.MAX_VALUE))
        val before = repository.events.first()
        assertTrue(runCatching { repository.applyBatch(setOf(first.id, overflow.id), BatchOperation.COMPLETE) }
            .exceptionOrNull() is ArithmeticException)
        assertEquals(before, repository.events.first())
    }

    @Test
    fun autoArchiveIncludesOnlyExpiredUnfinishedRowsAndInvalidatesOldEdits() = runBlocking {
        val due = repository.save(draft("截止边界").copy(deadlineAt = clock.millis()))
        val finished = repository.save(draft("已完成过期").copy(deadlineAt = clock.millis() - 60_000))
        repository.setCompleted(finished.id, true)
        val future = repository.save(draft("将来"))
        val priorArchive = repository.save(draft("已归档").copy(deadlineAt = clock.millis() - 60_000))
        repository.setArchived(priorArchive.id, true)
        val archivedBefore = repository.events.first().first { it.id == priorArchive.id }
        assertEquals(0, repository.applyExpiredPolicy())
        repository.setExpiredPolicy(ExpiredPolicy.AUTO_ARCHIVE)
        assertEquals(1, repository.applyExpiredPolicy())
        assertEquals(0, repository.applyExpiredPolicy())
        val rows = repository.events.first().associateBy { it.id }
        assertNotNull(rows.getValue(due.id).archivedAt)
        assertEquals(due.revision + 1, rows.getValue(due.id).revision)
        assertNull(rows.getValue(finished.id).archivedAt)
        assertEquals(future, rows.getValue(future.id))
        assertEquals(archivedBefore, rows.getValue(priorArchive.id))
        assertTrue(runCatching { repository.save(due.edit().copy(title = "旧编辑")) }
            .exceptionOrNull() is EventConflictException)
    }

    @Test
    fun manualCleanupArchivesExpiredCompletedItemsAndKeepsCompletionFlag() = runBlocking {
        val overdue = repository.save(draft().copy(deadlineAt = clock.millis() - 60_000))
        repository.setCompleted(overdue.id, true)
        val completed = repository.events.first().single()
        assertEquals(1, repository.archiveExpired())
        val archived = repository.events.first().single()
        assertEquals(completed.completedAt, archived.completedAt)
        assertEquals(completed.revision + 1, archived.revision)
        assertNotNull(archived.archivedAt)
        assertEquals(0, repository.archiveExpired())
        repository.setArchived(overdue.id, false)
        assertEquals(completed.completedAt, repository.events.first().single().completedAt)
    }

    @Test
    fun settingsUpdatesAreIndependentAndAtomicValidationPreventsPartialSave() = runBlocking {
        repository.setUrgentMinutes(720)
        repository.setExpiredPolicy(ExpiredPolicy.AUTO_ARCHIVE)
        assertEquals(AppSettings(720, ExpiredPolicy.AUTO_ARCHIVE), repository.settings.first())
        repository.setUrgentMinutes(60)
        assertEquals(AppSettings(60, ExpiredPolicy.AUTO_ARCHIVE), repository.settings.first())
        repository.setExpiredPolicy(ExpiredPolicy.KEEP)
        assertEquals(AppSettings(60, ExpiredPolicy.KEEP), repository.settings.first())
        assertTrue(runCatching { repository.saveSettings(AppSettings(0, ExpiredPolicy.AUTO_ARCHIVE)) }
            .exceptionOrNull() is EventValidationException)
        assertEquals(AppSettings(60, ExpiredPolicy.KEEP), repository.settings.first())
        repository.saveSettings(AppSettings(120, ExpiredPolicy.AUTO_ARCHIVE))
        assertEquals(AppSettings(120, ExpiredPolicy.AUTO_ARCHIVE), repository.settings.first())
    }

    @Test
    fun attachmentIdsAndPathsCannotBeBorrowedFromAnotherEvent() = runBlocking {
        val image = attachment()
        val owner = repository.save(draft("拥有者").copy(attachments = listOf(image)))
        val other = repository.save(draft("另一项").copy(customFields = listOf(CustomField("note", "备注", "原值"))))
        listOf(image, image.copy(id = UUID.randomUUID().toString()), image.copy(relativePath = "${UUID.randomUUID()}.img"))
            .forEach { borrowed ->
                assertTrue(runCatching { repository.save(other.edit().copy(title = "不应保存", attachments = listOf(borrowed))) }
                    .exceptionOrNull() is EventValidationException)
                assertEquals(other, repository.events.first().first { it.id == other.id })
            }
        assertEquals(owner, repository.events.first().first { it.id == owner.id })
        val removed = repository.save(owner.edit().copy(attachments = emptyList()))
        assertTrue(removed.attachments.isEmpty())
        assertTrue(repository.referencedAttachmentPaths().isEmpty())
    }

    @Test
    fun staleEditorCannotEraseNewCustomFieldsOrAttachments() = runBlocking {
        val original = repository.save(draft())
        val updated = repository.save(original.edit().copy(
            customFields = listOf(CustomField("url", "网址", "https://example.org")), attachments = listOf(attachment()),
        ))
        assertTrue(runCatching { repository.save(original.edit().copy(title = "旧窗口")) }
            .exceptionOrNull() is EventConflictException)
        assertEquals(updated, repository.events.first().single())
    }

    @Test
    fun observedRelationsAreCompleteSnapshotsDuringEdits() = runBlocking {
        val tag = repository.saveTag(null, "项目")
        val old = repository.save(draft().copy(customFields = listOf(CustomField("one", "地点", "旧地点")),
            attachments = listOf(attachment("old.png"))))
        val changed = old.edit().copy(title = "全部更新", tagIds = setOf(tag.id),
            customFields = listOf(CustomField("two", "联系人", "同学")), attachments = listOf(attachment("new.png")))
        withTimeout(10_000) {
            val started = CompletableDeferred<Unit>()
            val observed = mutableListOf<DdlEvent>()
            val observer = async {
                repository.events.onEach { rows ->
                    observed += rows.single()
                    started.complete(Unit)
                }.first { it.single().revision == old.revision + 1 }
            }
            started.await()
            val current = repository.save(changed)
            assertEquals(current, observer.await().single())
            assertTrue(observed.all { it == old || it == current })
        }
    }

    private class MutableClock(var current: Instant) : Clock() {
        override fun instant(): Instant = current
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(current, zone)
    }
}
