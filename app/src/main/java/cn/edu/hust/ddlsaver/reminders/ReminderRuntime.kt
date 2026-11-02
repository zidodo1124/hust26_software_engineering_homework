package cn.edu.hust.ddlsaver.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import cn.edu.hust.ddlsaver.data.SaverDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Application-context lifetime, independent from the screen ViewModel's database connection. */
class ReminderRuntime private constructor(private val app: Context) {
    private val platform = AndroidReminderPlatform(app)
    private val database = SaverDatabase.create(app)
    private val engine = ReminderEngine(database, platform)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val mutableHealth = MutableStateFlow(platform.capabilities())
    val health: StateFlow<ReminderHealth> = mutableHealth.asStateFlow()

    init {
        scope.launch { for (ignored in requests) reconcileNow() }
    }

    fun requestReconcile() {
        try { mutableHealth.value = platform.capabilities().copy(
            lastError = mutableHealth.value.lastError, pendingCount = mutableHealth.value.pendingCount) }
        catch (error: Exception) { failure(error) }
        requests.trySend(Unit)
    }

    suspend fun reconcileNow(): ReminderHealth {
        return try { engine.reconcile(forceReschedule = true).also { mutableHealth.value = it } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { failure(error) }
    }

    private fun failure(error: Exception): ReminderHealth {
        val failed = mutableHealth.value.copy(
            lastError = "提醒恢复暂未完成（${error.javaClass.simpleName}），下次打开时将重试。")
        mutableHealth.value = failed
        Log.e("DDLReminder", "Reminder reconciliation failed: ${error.javaClass.simpleName}")
        return failed
    }

    private fun close() { scope.cancel(); requests.close(); database.close() }

    companion object {
        @Volatile private var instance: ReminderRuntime? = null
        @Synchronized fun get(context: Context): ReminderRuntime {
            val app = context.applicationContext
            if (instance?.app !== app) { instance?.close(); instance = ReminderRuntime(app) }
            return checkNotNull(instance)
        }
        /** Close real resources between Robolectric applications; production still follows the same path. */
        @Synchronized fun resetForTests() { instance?.close(); instance = null }
    }
}

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != AndroidReminderPlatform.ACTION_WAKE) return
        reconcileBounded(context, RecoveryTrigger.ALARM)
    }
}

class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val trigger = RecoveryTrigger.fromAction(intent?.action) ?: return
        if (trigger == RecoveryTrigger.ALARM) return
        reconcileBounded(context, trigger)
    }
}

private fun BroadcastReceiver.reconcileBounded(context: Context, trigger: RecoveryTrigger) {
    val pending = goAsync()
    val app = context.applicationContext
    val diagnostics = ReminderRecoveryDiagnostics.create(app, trigger)
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        runReceiverReconciliation(diagnostics, { ReminderRuntime.get(app).reconcileNow() }, { pending.finish() })
    }
}

/** IO-worker body shared by manifest receivers. Completion is guaranteed even if diagnostics fail. */
internal suspend fun runReceiverReconciliation(
    diagnostics: ReminderRecoveryDiagnostics?,
    reconcile: suspend () -> ReminderHealth,
    finish: () -> Unit,
    timeoutMillis: Long = 8_000L,
) {
    try {
        withTimeout(timeoutMillis) {
            diagnostics?.received()
            diagnostics?.started()
            val health = reconcile()
            val outcome = when {
                health.lastError != null -> RecoveryOutcome.HEALTH_ERROR
                !health.canPost -> RecoveryOutcome.NOTIFICATION_BLOCKED
                else -> RecoveryOutcome.COMPLETED
            }
            diagnostics?.finished(outcome, health)
        }
    } catch (_: TimeoutCancellationException) {
        diagnostics?.finished(RecoveryOutcome.TIMED_OUT)
        Log.e("DDLReminder", "Receiver reconciliation timed out")
    } catch (cancelled: CancellationException) {
        diagnostics?.finished(RecoveryOutcome.CANCELLED)
        throw cancelled
    } catch (_: Exception) {
        // Durable plans/control remain pending. Never retain exception messages or user contents.
        diagnostics?.finished(RecoveryOutcome.FAILED)
        Log.e("DDLReminder", "Receiver reconciliation deferred")
    } finally {
        finish()
    }
}
