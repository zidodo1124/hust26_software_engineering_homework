package cn.edu.hust.ddlsaver.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cn.edu.hust.ddlsaver.domain.AppSettings
import cn.edu.hust.ddlsaver.domain.EventConflictException
import cn.edu.hust.ddlsaver.domain.EventDraft
import cn.edu.hust.ddlsaver.domain.EventNotFoundException
import cn.edu.hust.ddlsaver.domain.EventValidationException
import cn.edu.hust.ddlsaver.domain.Importance
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
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
class RoomEventRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: SaverDatabase
    private lateinit var repository: RoomEventRepository
    private lateinit var databaseName: String
    private val clock = MutableClock(Instant.parse("2030-01-02T00:00:00Z"))

    @Before
    fun openDatabase() {
        context = ApplicationProvider.getApplicationContext()
        databaseName = "repository-test-${UUID.randomUUID()}.db"
        reopenDatabase()
    }

    private fun reopenDatabase() {
        database = Room.databaseBuilder(context, SaverDatabase::class.java, databaseName).build()
        repository = RoomEventRepository(database, clock)
    }

    @After
    fun closeDatabase() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    private fun draft(title: String = "课程报告") = EventDraft(
        title = title,
        deadlineAt = Instant.parse("2030-01-02T10:00:00Z").toEpochMilli(),
        sprintMinutes = 120,
        description = "提交代码与说明",
        importance = Importance.HIGH,
    )

    @Test
    fun eventAndSettingsSurviveClosingAndReopeningFileDatabase() = runBlocking {
        val saved = repository.save(draft())
        repository.setUrgentMinutes(48 * 60)
        database.close()
        reopenDatabase()
        assertEquals(saved, repository.events.first().single())
        assertEquals(AppSettings(48 * 60), repository.settings.first())
    }

    @Test
    fun editingPreservesIdentityCreationTimeAndIndependentStateFlags() = runBlocking {
        val saved = repository.save(draft())
        repository.setCompleted(saved.id, true)
        repository.setArchived(saved.id, true)
        val original = repository.events.first().single()
        clock.current = clock.current.plusSeconds(60)
        val edited = repository.save(draft("报告终稿").copy(id = saved.id, expectedRevision = original.revision))
        assertEquals(saved.id, edited.id)
        assertEquals(saved.createdAt, edited.createdAt)
        assertEquals(original.completedAt, edited.completedAt)
        assertEquals(original.archivedAt, edited.archivedAt)
        assertEquals(original.revision + 1, edited.revision)
        assertTrue(edited.updatedAt > original.updatedAt)
        assertEquals("报告终稿", edited.title)
    }

    @Test
    fun restoringArchiveDoesNotMarkCompletedEventAsUnfinished() = runBlocking {
        val saved = repository.save(draft())
        repository.setCompleted(saved.id, true)
        repository.setArchived(saved.id, true)
        repository.setArchived(saved.id, false)
        val restored = repository.events.first().single()
        assertNotNull(restored.completedAt)
        assertNull(restored.archivedAt)
        assertEquals(saved.deadlineAt, restored.deadlineAt)
        repository.setCompleted(saved.id, false)
        assertNull(repository.events.first().single().completedAt)
    }

    @Test
    fun staleEditIsRejectedWithoutOverwritingNewerState() = runBlocking {
        val saved = repository.save(draft())
        repository.setCompleted(saved.id, true)
        val result = runCatching {
            repository.save(draft("旧编辑").copy(id = saved.id, expectedRevision = saved.revision))
        }
        assertTrue(result.exceptionOrNull() is EventConflictException)
        val current = repository.events.first().single()
        assertEquals(saved.title, current.title)
        assertNotNull(current.completedAt)
    }

    @Test
    fun deletedEventCannotBeResurrectedByStaleSaveOrStateChange() = runBlocking {
        val saved = repository.save(draft())
        repository.delete(saved.id)
        val edit = runCatching {
            repository.save(draft().copy(id = saved.id, expectedRevision = saved.revision))
        }
        val complete = runCatching { repository.setCompleted(saved.id, true) }
        assertTrue(edit.exceptionOrNull() is EventNotFoundException)
        assertTrue(complete.exceptionOrNull() is EventNotFoundException)
        assertTrue(repository.events.first().isEmpty())
    }

    @Test
    fun invalidSaveAndSettingLeaveExistingDataUntouched() = runBlocking {
        val saved = repository.save(draft())
        repository.setUrgentMinutes(720)
        val edit = runCatching {
            repository.save(draft(" ").copy(id = saved.id, expectedRevision = saved.revision))
        }
        val setting = runCatching { repository.setUrgentMinutes(0) }
        assertTrue(edit.exceptionOrNull() is EventValidationException)
        assertTrue(setting.exceptionOrNull() is EventValidationException)
        assertEquals(saved, repository.events.first().single())
        assertEquals(AppSettings(720), repository.settings.first())
    }

    @Test
    fun flowEmitsSavedAndUpdatedRowsInDeadlineOrder() = runBlocking {
        val later = repository.save(draft("较晚"))
        val earlier = repository.save(draft("较早").copy(deadlineAt = later.deadlineAt - 60_000))
        assertEquals(listOf(earlier.id, later.id), repository.events.first().map { it.id })
        withTimeout(10_000) {
            val initialEmission = CompletableDeferred<Unit>()
            val observation = async {
                repository.events.onEach { initialEmission.complete(Unit) }
                    .first { rows -> rows.any { it.id == earlier.id && it.completedAt != null } }
            }
            initialEmission.await()
            repository.setCompleted(earlier.id, true)
            assertNotNull(observation.await().first { it.id == earlier.id }.completedAt)
        }
    }

    @Test
    fun concurrentEditsFromSameRevisionCannotBothSucceed() = runBlocking {
        val saved = repository.save(draft())
        val results = supervisorScope {
            listOf("版本甲", "版本乙").map { title ->
                async { runCatching { repository.save(draft(title).copy(id = saved.id, expectedRevision = saved.revision)) } }
            }.map { it.await() }
        }
        assertEquals(1, results.count { it.isSuccess })
        assertEquals(1, results.count { it.exceptionOrNull() is EventConflictException })
        assertEquals(saved.revision + 1, repository.events.first().single().revision)
    }

    @Test
    fun repeatedStateRequestIsIdempotent() = runBlocking {
        val saved = repository.save(draft())
        repository.setCompleted(saved.id, true)
        val completed = repository.events.first().single()
        clock.current = clock.current.plusSeconds(30)
        repository.setCompleted(saved.id, true)
        assertEquals(completed, repository.events.first().single())
        assertFalse(repository.events.first().isEmpty())
    }

    @Test
    fun newDatabaseUsesDefaultUrgentThreshold() = runBlocking {
        assertEquals(AppSettings(1440), repository.settings.first())
    }

    private class MutableClock(var current: Instant) : Clock() {
        override fun instant(): Instant = current
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(current, zone)
    }
}
