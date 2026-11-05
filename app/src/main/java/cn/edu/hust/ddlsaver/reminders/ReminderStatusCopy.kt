package cn.edu.hust.ddlsaver.reminders

import cn.edu.hust.ddlsaver.domain.AppSettings

/** 首页／设置共用的提醒能力说明，须与 ReminderEngine／AndroidReminderPlatform 行为一致。 */
object ReminderStatusCopy {
    fun anyReminderEnabled(settings: AppSettings): Boolean =
        settings.dailySummaryEnabled || settings.dangerReminderEnabled || settings.deadlineReminderEnabled

    /** 首页横幅；无问题时返回 null。 */
    fun homeWarning(settings: AppSettings, health: ReminderHealth): String? {
        if (!anyReminderEnabled(settings)) return null
        health.lastError?.let { return "提醒没安排好，到“设置”里检查一下：$it" }
        if (!health.canPost) {
            return "系统通知没开，或提醒通道已关闭。到“设置”里检查通知权限和提醒通道。"
        }
        if (!health.canScheduleExact) {
            return "现在使用近似提醒，通知可能晚一些。可到“设置”里允许精确提醒。"
        }
        return null
    }

    /** 设置页：系统尚不可发通知时，说明计划仍保留。 */
    fun settingsPlansRetainedHint(health: ReminderHealth): String? =
        if (!health.canPost) {
            "提醒计划会保留。开启系统通知前，不会弹出通知，也不会安排系统唤醒。"
        } else null
}
