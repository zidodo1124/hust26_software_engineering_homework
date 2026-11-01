package cn.edu.hust.ddlsaver.reminders

import cn.edu.hust.ddlsaver.domain.AppSettings
import org.junit.Assert.*
import org.junit.Test

/** 岳羽泉：权限／能力文案须与 canPost、canScheduleExact 及引擎暂停唤醒行为一致。 */
class ReminderStatusCopyTest {
    private val enabled = AppSettings()
    private val disabled = AppSettings(
        dailySummaryEnabled = false,
        dangerReminderEnabled = false,
        deadlineReminderEnabled = false,
    )

    @Test fun homeWarningSilentWhenAllReminderSwitchesOff() {
        assertNull(ReminderStatusCopy.homeWarning(disabled, ReminderHealth(canPost = false)))
    }

    @Test fun homeWarningPrefersLastErrorThenPostThenExact() {
        val err = ReminderHealth(canPost = true, canScheduleExact = true, lastError = "安排失败")
        assertTrue(ReminderStatusCopy.homeWarning(enabled, err)!!.contains("重新检查"))
        val noPost = ReminderHealth(canPost = false, canScheduleExact = true)
        val postMsg = ReminderStatusCopy.homeWarning(enabled, noPost)!!
        assertTrue(postMsg.contains("通知权限") || postMsg.contains("提醒通道"))
        assertTrue(postMsg.contains("通道"))
        val approx = ReminderHealth(canPost = true, canScheduleExact = false)
        assertTrue(ReminderStatusCopy.homeWarning(enabled, approx)!!.contains("近似提醒"))
        assertNull(ReminderStatusCopy.homeWarning(enabled, ReminderHealth(canPost = true, canScheduleExact = true)))
    }

    @Test fun settingsHintOnlyWhenCannotPost() {
        assertNotNull(ReminderStatusCopy.settingsPlansRetainedHint(ReminderHealth(canPost = false)))
        assertNull(ReminderStatusCopy.settingsPlansRetainedHint(ReminderHealth(canPost = true)))
        assertTrue(ReminderStatusCopy.settingsPlansRetainedHint(ReminderHealth(canPost = false))!!
            .contains("不会安排系统唤醒"))
    }
}
