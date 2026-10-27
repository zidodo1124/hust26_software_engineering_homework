package cn.edu.hust.ddlsaver.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import cn.edu.hust.ddlsaver.MainActivity

class AndroidReminderPlatform(context: Context) : ReminderPlatform {
    private val app = context.applicationContext
    private val alarms = app.getSystemService(AlarmManager::class.java)
    private val notifications = app.getSystemService(NotificationManager::class.java)

    init { ensureChannel() }

    private fun ensureChannel() {
        notifications.createNotificationChannel(NotificationChannel(REMINDER_CHANNEL, "DDL 提醒",
            NotificationManager.IMPORTANCE_DEFAULT).apply { description = "每日汇总、速通危险期及临近截止提醒" })
    }

    override fun capabilities(): ReminderHealth = ReminderHealth(
        canPost = (Build.VERSION.SDK_INT < 33 ||
            app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            notifications.areNotificationsEnabled() &&
            notifications.getNotificationChannel(REMINDER_CHANNEL)?.importance?.let { it != NotificationManager.IMPORTANCE_NONE } == true,
        canScheduleExact = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms(),
    )

    private fun alarmIntent() = Intent(app, ReminderAlarmReceiver::class.java)
        .setAction(ACTION_WAKE).setData(Uri.parse(ALARM_IDENTITY))

    override fun cancelAlarm() {
        val pending = PendingIntent.getBroadcast(app, 0, alarmIntent(),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        alarms.cancel(pending)
        // Retain the token identity; a future set() replaces it and stale callbacks still revalidate DB.
    }

    override fun scheduleAlarm(triggerAt: Long, exact: Boolean): String {
        val pending = PendingIntent.getBroadcast(app, 0, alarmIntent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (exact && (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms())) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                return "exact"
            } catch (error: SecurityException) {
                if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) throw error
            }
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        return "approximate"
    }

    override fun post(notification: ReminderNotification) {
        if (Build.VERSION.SDK_INT >= 33 &&
            app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Notifications unavailable")
        }
        check(capabilities().canPost) { "Notifications unavailable" }
        val route = Intent(app, MainActivity::class.java).setAction(ACTION_OPEN)
            .setData(Uri.Builder().scheme("ddl-saver").authority("notification")
                .appendPath(notification.tag).build())
            .putExtra(EXTRA_TODAY, notification.today)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        notification.eventId?.let { route.putExtra(EXTRA_EVENT_ID, it) }
        val click = PendingIntent.getActivity(app, 0, route,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = Notification.Builder(app, REMINDER_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(notification.title).setContentText(notification.text)
            .setContentIntent(click).setAutoCancel(true).setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE).setCategory(Notification.CATEGORY_REMINDER)
        if (notification.lines.isNotEmpty()) {
            val style = Notification.InboxStyle().setSummaryText(notification.text)
            notification.lines.forEach { style.addLine(it) }
            builder.setStyle(style)
        } else builder.setStyle(Notification.BigTextStyle().bigText(notification.text))
        notifications.notify(notification.tag, 0, builder.build())
    }

    override fun cancelNotification(tag: String) { notifications.cancel(tag, 0) }

    companion object {
        const val ACTION_WAKE = "cn.edu.hust.ddlsaver.REMINDER_WAKE"
        const val ACTION_OPEN = "cn.edu.hust.ddlsaver.OPEN_REMINDER"
        const val EXTRA_EVENT_ID = "reminder_event_id"
        const val EXTRA_TODAY = "reminder_today"
    }
}
