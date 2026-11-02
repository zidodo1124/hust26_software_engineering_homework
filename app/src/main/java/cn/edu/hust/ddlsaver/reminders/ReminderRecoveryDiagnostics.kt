package cn.edu.hust.ddlsaver.reminders

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicLongArray
import java.util.concurrent.atomic.AtomicReferenceArray

/** Fixed keys retain the latest BOOT attempt independently from later alarm/time/permission events. */
internal enum class RecoveryTrigger(val key: String) {
    BOOT("boot"), TIME_CHANGED("time_changed"), TIMEZONE_CHANGED("timezone_changed"),
    PACKAGE_REPLACED("package_replaced"), EXACT_PERMISSION("exact_permission"), ALARM("alarm");

    companion object {
        fun fromAction(action: String?): RecoveryTrigger? = when (action) {
            Intent.ACTION_BOOT_COMPLETED -> BOOT
            Intent.ACTION_TIME_CHANGED -> TIME_CHANGED
            Intent.ACTION_TIMEZONE_CHANGED -> TIMEZONE_CHANGED
            Intent.ACTION_MY_PACKAGE_REPLACED -> PACKAGE_REPLACED
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> EXACT_PERMISSION
            AndroidReminderPlatform.ACTION_WAKE -> ALARM
            else -> null
        }
    }
}

internal enum class RecoveryOutcome { COMPLETED, NOTIFICATION_BLOCKED, HEALTH_ERROR, TIMED_OUT, CANCELLED, FAILED }

internal data class RecoveryDiagnosticSnapshot(
    val trigger: RecoveryTrigger,
    val attempt: Long,
    val sequence: Long,
    val json: String,
    val write: (String, String) -> Unit,
)

/**
 * One independent serial writer; at most six queued snapshots plus one in-flight write. Submission
 * only replaces an in-memory slot and signals a conflated channel. A stalled disk cannot hold up
 * receiver reconciliation/finish. Intermediate phases may coalesce; process death can lose queued
 * evidence. This is deliberately best-effort diagnostics, not a durable business-work queue.
 */
internal class RecoveryDiagnosticWriter(
    private val isCurrent: (RecoveryTrigger, Long) -> Boolean = { trigger, attempt -> ReminderRecoveryDiagnostics.isCurrent(trigger, attempt) },
) : AutoCloseable {
    private data class Stamp(val attempt: Long, val sequence: Long)
    private val queued = ConcurrentHashMap<RecoveryTrigger, RecoveryDiagnosticSnapshot>()
    private val latestSubmitted = AtomicReferenceArray<Stamp>(RecoveryTrigger.entries.size)
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val draining = AtomicBoolean(false)

    init {
        scope.launch {
            for (ignored in wake) {
                draining.set(true)
                try {
                    do {
                        for (trigger in RecoveryTrigger.entries) {
                            val snapshot = queued.remove(trigger) ?: continue
                            try {
                                if (isCurrent(snapshot.trigger, snapshot.attempt)) {
                                    snapshot.write(trigger.key, snapshot.json)
                                }
                            } catch (_: Exception) {
                                // A failed write must not kill the writer or affect business work.
                            }
                        }
                    } while (queued.isNotEmpty())
                } finally {
                    draining.set(false)
                }
            }
        }
    }

    fun submit(snapshot: RecoveryDiagnosticSnapshot) {
        queued.compute(snapshot.trigger) { _, previous ->
            val latest = latestSubmitted.get(snapshot.trigger.ordinal)
            if (latest == null || snapshot.attempt > latest.attempt ||
                (snapshot.attempt == latest.attempt && snapshot.sequence > latest.sequence)) {
                latestSubmitted.set(snapshot.trigger.ordinal, Stamp(snapshot.attempt, snapshot.sequence))
                snapshot
            } else previous
        }
        wake.trySend(Unit)
    }

    internal val queuedCount: Int get() = queued.size
    internal val isIdle: Boolean get() = queued.isEmpty() && !draining.get()

    override fun close() { wake.close(); scope.cancel() }
}

/**
 * Debug-only, app-private evidence, never business data or exception text. Create on receiver entry;
 * record methods only enqueue in-memory snapshots. An absent record cannot prove non-delivery:
 * the process or storage may fail before an independent asynchronous write. COMPLETED describes
 * reconciliation, not proof that the OS retained an alarm or delivered a notification.
 */
internal class ReminderRecoveryDiagnostics private constructor(
    private val trigger: RecoveryTrigger,
    private val token: Long,
    private val receivedAt: Long,
    private val receivedElapsed: Long,
    private val writer: RecoveryDiagnosticWriter,
    private val write: (String, String) -> Unit,
) {
    private var startedAt: Long? = null

    fun received() = record("received")

    fun started() {
        startedAt = System.currentTimeMillis()
        record("started")
    }

    fun finished(outcome: RecoveryOutcome, health: ReminderHealth? = null) = record("finished", outcome, health)

    private fun record(phase: String, outcome: RecoveryOutcome? = null, health: ReminderHealth? = null) {
        try {
            if (!isCurrent(trigger, token)) return
            val record = JSONObject().put("version", 1).put("trigger", trigger.key)
                .put("phase", phase).put("received_at", receivedAt)
                .put("received_elapsed_ms", receivedElapsed)
            startedAt?.let { record.put("started_at", it) }
            if (outcome != null) {
                record.put("finished_at", System.currentTimeMillis())
                    .put("elapsed_ms", (SystemClock.elapsedRealtime() - receivedElapsed).coerceAtLeast(0))
                    .put("outcome", outcome.name.lowercase())
            }
            health?.let {
                record.put("can_post", it.canPost).put("can_schedule_exact", it.canScheduleExact)
                    .put("pending_count", it.pendingCount.coerceAtLeast(0))
                    .put("has_error", it.lastError != null)
            }
            writer.submit(RecoveryDiagnosticSnapshot(trigger, token, sequence.incrementAndGet(), record.toString(), write))
        } catch (_: Exception) {
            // A diagnostic failure must not skip reconciliation or pending-result completion.
        }
    }

    companion object {
        const val FILE_NAME = "reminder-recovery-debug"
        private val sequence = AtomicLong()
        private val latestTokens = AtomicLongArray(RecoveryTrigger.entries.size)
        internal val sharedWriter by lazy { RecoveryDiagnosticWriter() }

        internal fun isCurrent(trigger: RecoveryTrigger, attempt: Long): Boolean = latestTokens.get(trigger.ordinal) == attempt

        fun create(context: Context, trigger: RecoveryTrigger): ReminderRecoveryDiagnostics? = try {
            if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) null
            else create(trigger) { key, value ->
                // Lazily open preferences here, never on the main-thread receiver entry path.
                context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE).edit().putString(key, value).commit()
            }
        } catch (_: Exception) { null }

        /** Injection keeps storage failures testable without changing production reconciliation. */
        internal fun create(trigger: RecoveryTrigger, writer: RecoveryDiagnosticWriter = sharedWriter,
            write: (String, String) -> Unit): ReminderRecoveryDiagnostics {
            val token = synchronized(latestTokens) {
                sequence.incrementAndGet().also { latestTokens.set(trigger.ordinal, it) }
            }
            return ReminderRecoveryDiagnostics(trigger, token, System.currentTimeMillis(), SystemClock.elapsedRealtime(), writer, write)
        }
    }
}
