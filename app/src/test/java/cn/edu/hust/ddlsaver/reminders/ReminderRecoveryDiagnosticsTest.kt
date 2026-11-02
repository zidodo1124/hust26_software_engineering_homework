package cn.edu.hust.ddlsaver.reminders

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35])
class ReminderRecoveryDiagnosticsTest {
    private lateinit var context: Context
    private var originalFlags = 0
    private lateinit var writer: RecoveryDiagnosticWriter
    private val healthy = ReminderHealth(canPost = true, canScheduleExact = true, pendingCount = 2)

    @Before
    fun prepare() {
        context = ApplicationProvider.getApplicationContext()
        originalFlags = context.applicationInfo.flags
        context.applicationInfo.flags = originalFlags or ApplicationInfo.FLAG_DEBUGGABLE
        context.deleteSharedPreferences(ReminderRecoveryDiagnostics.FILE_NAME)
        writer = RecoveryDiagnosticWriter()
    }

    @After
    fun cleanup() = runBlocking {
        try {
            awaitWriter(writer)
            awaitWriter(ReminderRecoveryDiagnostics.sharedWriter)
        } finally {
            writer.close()
            context.applicationInfo.flags = originalFlags
            context.deleteSharedPreferences(ReminderRecoveryDiagnostics.FILE_NAME)
        }
    }

    @Test
    fun productionStorageIsOffMainThreadBoundedAndRetainsBootAcrossLaterTriggers() = runBlocking {
        var storageAccesses = 0
        val guarded = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                assertNotSame("Receiver entry must not open diagnostic storage", Looper.getMainLooper().thread, Thread.currentThread())
                storageAccesses++
                return super.getSharedPreferences(name, mode)
            }
        }
        // Factory is called at receiver entry; all storage operations must remain deferred.
        val boot = ReminderRecoveryDiagnostics.create(guarded, RecoveryTrigger.BOOT)
        assertNotNull(boot)
        assertEquals(0, storageAccesses)
        withContext(Dispatchers.IO) { runReceiverReconciliation(boot, { healthy }, {}) }
        awaitWriter(ReminderRecoveryDiagnostics.sharedWriter)
        val prefs = context.getSharedPreferences(ReminderRecoveryDiagnostics.FILE_NAME, Context.MODE_PRIVATE)
        val bootRecord = prefs.getString("boot", null)
        withContext(Dispatchers.IO) {
            RecoveryTrigger.entries.filter { it != RecoveryTrigger.BOOT }.forEach { trigger ->
                repeat(4) { runReceiverReconciliation(ReminderRecoveryDiagnostics.create(guarded, trigger), { healthy }, {}) }
            }
        }
        awaitWriter(ReminderRecoveryDiagnostics.sharedWriter)
        assertEquals(bootRecord, prefs.getString("boot", null))
        assertEquals(RecoveryTrigger.entries.map { it.key }.toSet(), prefs.all.keys)
        assertTrue(prefs.all.values.all { it is String && it.length < 1024 })
        assertTrue(storageAccesses > 0)
    }

    @Test
    fun returnedHealthErrorIsNotSuccessAndOnlyAllowlistedFieldsArePersisted() = runBlocking {
        val writes = CopyOnWriteArrayList<JSONObject>()
        val diagnostics = ReminderRecoveryDiagnostics.create(RecoveryTrigger.BOOT, writer) { _, value -> writes += JSONObject(value) }
        val privateError = "private event title content://secret/image secret-token"
        runReceiverReconciliation(diagnostics, { healthy.copy(lastError = privateError) }, {})
        awaitWriter(writer)

        // Intermediate phases may coalesce, but the final snapshot includes all captured times.
        val result = writes.last()
        assertEquals("finished", result.getString("phase"))
        assertEquals("health_error", result.getString("outcome"))
        assertTrue(result.getBoolean("has_error"))
        assertEquals(2, result.getInt("pending_count"))
        assertFalse(result.toString().contains(privateError))
        assertEquals(setOf("version", "trigger", "phase", "received_at", "received_elapsed_ms", "started_at",
            "finished_at", "elapsed_ms", "outcome", "can_post", "can_schedule_exact", "pending_count", "has_error"),
            result.keys().asSequence().toSet())
    }

    @Test
    fun notificationBlockedIsDistinctFromCompletedApproximateScheduling() = runBlocking {
        val results = CopyOnWriteArrayList<JSONObject>()
        listOf(healthy.copy(canPost = false), healthy.copy(canScheduleExact = false)).forEach { health ->
            val diagnostics = ReminderRecoveryDiagnostics.create(RecoveryTrigger.EXACT_PERMISSION, writer) { _, value ->
                val json = JSONObject(value)
                if (json.getString("phase") == "finished") results += json
            }
            runReceiverReconciliation(diagnostics, { health }, {})
            awaitWriter(writer)
        }
        assertEquals("notification_blocked", results[0].getString("outcome"))
        assertEquals("completed", results[1].getString("outcome"))
        assertFalse(results[1].getBoolean("can_schedule_exact"))
    }

    @Test
    fun olderOverlappingAttemptCannotOverwriteNewerAttempt() = runBlocking {
        val values = ConcurrentHashMap<String, String>()
        val old = ReminderRecoveryDiagnostics.create(RecoveryTrigger.BOOT, writer) { key, value -> values[key] = value }
        old.received()
        val fresh = ReminderRecoveryDiagnostics.create(RecoveryTrigger.BOOT, writer) { key, value -> values[key] = value }
        fresh.received()
        fresh.started()
        fresh.finished(RecoveryOutcome.COMPLETED, healthy)
        old.finished(RecoveryOutcome.FAILED)
        awaitWriter(writer)
        assertEquals("completed", JSONObject(checkNotNull(values["boot"])).getString("outcome"))
    }

    @Test
    fun storageFailureCannotSkipReconciliationOrPendingResultCompletion() = runBlocking {
        var reconciled = 0
        var finished = 0
        val writeAttempts = AtomicInteger()
        val diagnostics = ReminderRecoveryDiagnostics.create(RecoveryTrigger.BOOT, writer) { _, _ ->
            writeAttempts.incrementAndGet()
            throw IOException("private path")
        }
        runReceiverReconciliation(diagnostics, { reconciled++; healthy }, { finished++ })
        assertEquals(1, reconciled)
        assertEquals(1, finished)
        awaitWriter(writer)
        assertTrue(writeAttempts.get() > 0)
        // A failure must not kill the independent writer for subsequent recovery evidence.
        val laterWrites = CopyOnWriteArrayList<String>()
        val later = ReminderRecoveryDiagnostics.create(RecoveryTrigger.BOOT, writer) { _, value -> laterWrites += value }
        runReceiverReconciliation(later, { healthy }, {})
        awaitWriter(writer)
        assertEquals("completed", JSONObject(laterWrites.last()).getString("outcome"))
    }

    @Test
    fun timeoutAndThrownFailureAreRecordedAndEachFinishExactlyOnce() = runBlocking {
        val outcomes = CopyOnWriteArrayList<String>()
        var finished = 0
        fun recorder() = ReminderRecoveryDiagnostics.create(RecoveryTrigger.BOOT, writer) { _, value ->
            JSONObject(value).takeIf { it.has("outcome") }?.let { outcomes += it.getString("outcome") }
        }
        runReceiverReconciliation(recorder(), { delay(1_000); healthy }, { finished++ }, timeoutMillis = 10)
        awaitWriter(writer)
        runReceiverReconciliation(recorder(), { throw SecurityException("private uri secret-token") }, { finished++ })
        awaitWriter(writer)
        assertEquals(listOf("timed_out", "failed"), outcomes)
        assertEquals(2, finished)
    }

    @Test
    fun cancellationIsNotSwallowedAndStillFinishesPendingResult() = runBlocking {
        var outcome: String? = null
        var finished = 0
        val diagnostics = ReminderRecoveryDiagnostics.create(RecoveryTrigger.BOOT, writer) { _, value ->
            JSONObject(value).takeIf { it.has("outcome") }?.let { outcome = it.getString("outcome") }
        }
        val expected = CancellationException("cancelled")
        try {
            runReceiverReconciliation(diagnostics, { throw expected }, { finished++ })
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) { assertEquals("cancelled", actual.message) }
        awaitWriter(writer)
        assertEquals("cancelled", outcome)
        assertEquals(1, finished)
    }

    @Test
    fun releaseBuildDoesNotCreateOrOpenDiagnosticStorage() = runBlocking {
        context.applicationInfo.flags = originalFlags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        val guarded = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                throw AssertionError("Release diagnostics must not open storage")
        }
        val diagnostics = ReminderRecoveryDiagnostics.create(guarded, RecoveryTrigger.BOOT)
        assertNull(diagnostics)
        var finished = false
        runReceiverReconciliation(diagnostics, { healthy }, { finished = true })
        assertTrue(finished)
        assertFalse(context.dataDir.resolve("shared_prefs/${ReminderRecoveryDiagnostics.FILE_NAME}.xml").exists())
    }

    @Test
    fun stalledWriterDoesNotDelayReconciliationOrFinishAndQueueRetainsLatestSixTriggers() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val first = AtomicBoolean(true)
        val persisted = ConcurrentHashMap<String, String>()
        val persist: (String, String) -> Unit = { key, value ->
            if (first.compareAndSet(true, false)) {
                entered.countDown()
                check(release.await(15, TimeUnit.SECONDS)) { "Test writer release timed out" }
            }
            persisted[key] = value
        }
        val executor = Executors.newSingleThreadExecutor()
        try {
            ReminderRecoveryDiagnostics.create(RecoveryTrigger.ALARM, writer, persist).received()
            assertTrue("Expected the independent disk writer to enter", entered.await(5, TimeUnit.SECONDS))
            val oldBoot = ReminderRecoveryDiagnostics.create(RecoveryTrigger.BOOT, writer, persist)
            oldBoot.received()
            val didReconcile = AtomicBoolean()
            val didFinish = AtomicBoolean()
            val completed = executor.submit<Boolean> {
                runBlocking {
                    val boot = ReminderRecoveryDiagnostics.create(RecoveryTrigger.BOOT, writer, persist)
                    runReceiverReconciliation(boot, { didReconcile.set(true); healthy }, { didFinish.set(true) })
                }
                true
            }
            assertTrue("Reconciliation and finish must complete while disk is still blocked", completed.get(5, TimeUnit.SECONDS))
            assertTrue(didReconcile.get())
            assertTrue(didFinish.get())
            assertEquals(1L, release.count)
            RecoveryTrigger.entries.filter { it != RecoveryTrigger.BOOT }.forEach { trigger ->
                repeat(20) {
                    val diagnostic = ReminderRecoveryDiagnostics.create(trigger, writer, persist)
                    runReceiverReconciliation(diagnostic, { healthy }, {})
                }
            }
            oldBoot.finished(RecoveryOutcome.FAILED)
            assertEquals(6, writer.queuedCount)
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
        awaitWriter(writer)
        assertEquals(RecoveryTrigger.entries.map { it.key }.toSet(), persisted.keys)
        assertTrue(persisted.values.all { JSONObject(it).getString("outcome") == "completed" })
    }

    @Test
    fun lateOlderSnapshotCannotReplaceNewerQueuedAttemptOrPhase() = runBlocking {
        // Force the producer check/submit race deterministically at the writer boundary.
        val orderingWriter = RecoveryDiagnosticWriter { _, _ -> true }
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val persisted = ConcurrentHashMap<String, String>()
        try {
            orderingWriter.submit(RecoveryDiagnosticSnapshot(RecoveryTrigger.ALARM, 1, 1, "blocked") { _, _ ->
                entered.countDown()
                check(release.await(15, TimeUnit.SECONDS))
            })
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val persist: (String, String) -> Unit = { key, value -> persisted[key] = value }
            orderingWriter.submit(RecoveryDiagnosticSnapshot(RecoveryTrigger.BOOT, 20, 30, "new", persist))
            orderingWriter.submit(RecoveryDiagnosticSnapshot(RecoveryTrigger.BOOT, 19, 99, "old attempt", persist))
            orderingWriter.submit(RecoveryDiagnosticSnapshot(RecoveryTrigger.BOOT, 20, 29, "old phase", persist))
            assertEquals(1, orderingWriter.queuedCount)
            release.countDown()
            awaitWriter(orderingWriter)
            assertEquals("new", persisted["boot"])
            // Preserve the version stamp after dequeue too: a late phase cannot regress disk state.
            orderingWriter.submit(RecoveryDiagnosticSnapshot(RecoveryTrigger.BOOT, 20, 29, "late old phase", persist))
            awaitWriter(orderingWriter)
            assertEquals("new", persisted["boot"])
        } finally {
            release.countDown()
            orderingWriter.close()
        }
    }

    private suspend fun awaitWriter(target: RecoveryDiagnosticWriter) = withTimeout(5_000) {
        while (!target.isIdle) delay(1)
    }
}
