package cn.edu.hust.ddlsaver

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import cn.edu.hust.ddlsaver.ui.SaverApp
import cn.edu.hust.ddlsaver.ui.SaverTheme
import cn.edu.hust.ddlsaver.ui.SaverViewModel

class MainActivity : ComponentActivity() {
    private lateinit var model: SaverViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this, SaverViewModel.factory(application))[SaverViewModel::class.java]
        if (savedInstanceState == null) {
            handleReminderIntent(intent)
        } else if (savedInstanceState.getBoolean("pending_reminder") && model.state.value.reminderNavigation == null) {
            model.requestReminderNavigation(savedInstanceState.getString("pending_reminder_event"),
                savedInstanceState.getBoolean("pending_reminder_today"))
            if (savedInstanceState.getBoolean("pending_reminder_deferred")) {
                model.state.value.reminderNavigation?.let { model.deferReminderNavigation(it.token) }
            }
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
        setContent {
            SaverTheme {
                SaverApp(model)
            }
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Consume deliveries into the ViewModel instead of replacing the Activity's launch identity.
        // Queued deliveries are saved explicitly below; recreation must not replay a consumed Intent.
        handleReminderIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("pending_reminder", model.state.value.reminderNavigation != null)
        model.state.value.reminderNavigation?.let {
            outState.putBoolean("pending_reminder", true)
            outState.putString("pending_reminder_event", it.eventId)
            outState.putBoolean("pending_reminder_today", it.today)
            outState.putBoolean("pending_reminder_deferred", it.deferred)
        }
        super.onSaveInstanceState(outState)
    }

    private fun handleReminderIntent(value: Intent?) {
        if (value?.action != "cn.edu.hust.ddlsaver.OPEN_REMINDER") return
        val eventId = value.getStringExtra("reminder_event_id")?.takeIf { it.isNotBlank() }
        val today = value.getBooleanExtra("reminder_today", false)
        if (eventId != null || today) model.requestReminderNavigation(eventId, eventId == null && today)
    }
}
