package cn.edu.hust.ddlsaver.ui

import android.content.Context
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import cn.edu.hust.ddlsaver.MainActivity
import cn.edu.hust.ddlsaver.data.RoomEventRepository
import cn.edu.hust.ddlsaver.data.SaverDatabase
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.EventDraft
import cn.edu.hust.ddlsaver.reminders.ReminderRuntime
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.SQLiteMode
import java.time.Instant
import java.util.concurrent.TimeUnit

/** Real Activity, ViewModel and Room; only the paused Android uptime clock is controlled. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class SaverViewModelFeedbackTest {
    private var controller: ActivityController<MainActivity>? = null
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before fun before() {
        ReminderRuntime.resetForTests()
        context.deleteDatabase("ddl-saver.db")
    }

    @After fun after() {
        controller?.pause()?.stop()?.destroy()
        ReminderRuntime.resetForTests()
        context.deleteDatabase("ddl-saver.db")
    }

    @Test
    fun onlySuccessfulCompletionShowsFeedbackAndRestoreClearsIt() {
        withRepository { save(EventDraft(title = "完成反馈事项", deadlineAt = Instant.now().plusSeconds(86400).toEpochMilli())) }
        controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val model = ViewModelProvider(checkNotNull(controller).get())[SaverViewModel::class.java]
        awaitState(model) { !it.loading && it.events.size == 1 }
        assertFalse(model.state.value.justCompleted)

        val requestAt = SystemClock.uptimeMillis()
        model.setCompleted(model.state.value.events.single())
        awaitState(model) { it.justCompleted && it.events.single().completedAt != null }
        val successObservedAt = SystemClock.uptimeMillis()
        println("FEEDBACK-CLOCK requestAt=$requestAt successObservedAt=$successObservedAt " +
            "observationWindow=${successObservedAt - requestAt}")
        assertTrue("Success observation must not precede the request", successObservedAt >= requestAt)
        assertTrue("The first observation must leave a strict pre-expiry observation point",
            successObservedAt - requestAt < 5_000)
        // The actual timer starts between requestAt and successObservedAt. Do not use the
        // later observation/DB-read time as if it were the timer origin.
        val strictBeforeExpiry = requestAt + 4_999
        advanceTo(strictBeforeExpiry, "strict-before-expiry")
        assertTrue("Production feedback must remain visible strictly before every possible 5000ms expiry",
            model.state.value.justCompleted)
        val latestPossibleExpiry = successObservedAt + 5_000
        advanceTo(latestPossibleExpiry, "latest-possible-expiry")
        assertFalse("Production feedback must end by the latest possible 5000ms expiry", model.state.value.justCompleted)
        assertNotNull(withRepository { eventsNow().single().completedAt })
        acknowledge(model)

        model.setCompleted(model.state.value.events.single())
        awaitState(model) { !it.busy && it.events.single().completedAt == null }
        assertFalse(model.state.value.justCompleted)
        acknowledge(model)
        model.setCompleted(model.state.value.events.single())
        awaitState(model) { it.justCompleted && it.events.single().completedAt != null }
        acknowledge(model)
        model.setCompleted(model.state.value.events.single())
        awaitState(model) { !it.busy && !it.justCompleted && it.events.single().completedAt == null }
        assertNull(model.state.value.operationError)
        acknowledge(model)

        val stale = model.state.value.events.single()
        withRepository { delete(stale.id) }
        model.setCompleted(stale)
        awaitState(model) { !it.busy && it.operationError != null }
        assertFalse("A failed completion must never celebrate", model.state.value.justCompleted)
        assertTrue(withRepository { eventsNow().isEmpty() })
    }

    private fun acknowledge(model: SaverViewModel) {
        model.state.value.actionResult?.let { model.acknowledgeAction(it.token) }
    }

    private fun advanceTo(target: Long, label: String) {
        val before = SystemClock.uptimeMillis()
        println("FEEDBACK-CLOCK phase=$label beforeAdvance=$before target=$target")
        assertTrue("Clock already passed $label; cannot make the required boundary observation", before <= target)
        shadowOf(Looper.getMainLooper()).idleFor(target - before, TimeUnit.MILLISECONDS)
        val after = SystemClock.uptimeMillis()
        println("FEEDBACK-CLOCK phase=$label afterAdvance=$after")
        assertEquals("The controlled Android clock must reach the requested observation point", target, after)
    }

    private fun awaitState(model: SaverViewModel, predicate: (SaverUiState) -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            // Drain only work due now: waiting for Room must not fast-forward the five-second timer.
            shadowOf(Looper.getMainLooper()).idle()
            if (predicate(model.state.value)) return
            Thread.sleep(10)
        }
        val state = model.state.value
        fail("Feedback state timeout: loading=${state.loading}, busy=${state.busy}, feedback=${state.justCompleted}, " +
            "completed=${state.events.singleOrNull()?.completedAt != null}, error=${state.operationError}")
    }

    private suspend fun RoomEventRepository.eventsNow(): List<DdlEvent> = events.first()

    private fun <T> withRepository(action: suspend RoomEventRepository.() -> T): T {
        val database = SaverDatabase.create(context)
        return try { runBlocking { RoomEventRepository(database).action() } } finally { database.close() }
    }
}
