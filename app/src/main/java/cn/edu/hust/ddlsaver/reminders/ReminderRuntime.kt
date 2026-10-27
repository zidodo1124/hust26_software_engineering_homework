package cn.edu.hust.ddlsaver.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.util.Log
import cn.edu.hust.ddlsaver.data.SaverDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    suspend fun reconcileNow() {
        try { mutableHealth.value = engine.reconcile(forceReschedule = true) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { failure(error) }
    }

    private fun failure(error: Exception) {
        mutableHealth.value = mutableHealth.value.copy(
            lastError = "提醒恢复暂未完成（${error.javaClass.simpleName}），下次打开时将重试。")
        Log.e("DDLReminder", "Reminder reconciliation failed: ${error.javaClass.simpleName}")
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
        reconcileBounded(context)
    }
}

class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action !in actions) return
        reconcileBounded(context)
    }
    companion object {
        private val actions = setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
    }
}

private fun BroadcastReceiver.reconcileBounded(context: Context) {
    val pending = goAsync()
    val app = context.applicationContext
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try { withTimeout(8_000L) { ReminderRuntime.get(app).reconcileNow() } }
        catch (error: Exception) {
            // Durable plans/control remain pending. Do not crash the process or log user event contents.
            Log.e("DDLReminder", "Receiver reconciliation deferred: ${error.javaClass.simpleName}")
        } finally { pending.finish() }
    }
}
