package cn.edu.hust.ddlsaver.reminders

import cn.edu.hust.ddlsaver.domain.AppSettings

/** 首页／设置共用的提醒能力说明，须与 ReminderEngine／AndroidReminderPlatform 行为一致。 */
object ReminderStatusCopy {
    fun anyReminderEnabled(settings: AppSettings): Boolean =
        settings.dailySummaryEnabled || settings.dangerReminderEnabled || settings.deadlineReminderEnabled

    /** 首页横幅；无问题时返回 null。 */
    fun homeWarning(settings: AppSettings, health: ReminderHealth): String? {
        if (!anyReminderEnabled(settings)) return null
        health.lastError?.let { return "提醒暂未安排完成，请前往设置重新检查：$it" }
        if (!health.canPost) {
            return "系统通知尚未开启或提醒通道已关闭。如需接收提醒，请前往“设置”检查通知权限与通道。"
        }
        if (!health.canScheduleExact) {
            return "当前使用近似提醒，通知可能延迟。可前往“设置”允许精确提醒。"
        }
        return null
    }

    /** 设置页：系统尚不可发通知时，说明计划仍保留。 */
    fun settingsPlansRetainedHint(health: ReminderHealth): String? =
        if (!health.canPost) {
            "提醒开关对应的计划会保留，但在系统通知可用前不会弹出通知，也不会安排系统唤醒。"
        } else null
}
