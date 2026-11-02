package cn.edu.hust.ddlsaver.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.edu.hust.ddlsaver.domain.AppSettings
import cn.edu.hust.ddlsaver.domain.CharacterStyle
import cn.edu.hust.ddlsaver.domain.ExpiredPolicy
import cn.edu.hust.ddlsaver.reminders.ReminderHealth
import cn.edu.hust.ddlsaver.reminders.ReminderStatusCopy

private const val SettingsMaxDurationMinutes = Long.MAX_VALUE / 60_000L

internal fun parseSummaryMinuteOfDay(value: String): Int? {
    if (!Regex("\\d{2}:\\d{2}").matches(value.trim())) return null
    val parts = value.trim().split(':')
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    return if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
}

@Composable
internal fun SettingsScreen(
    current: AppSettings,
    health: ReminderHealth,
    busy: Boolean,
    operationError: String?,
    onBack: () -> Unit,
    onSave: (AppSettings) -> Unit,
    onCategories: () -> Unit,
    onArchiveExpired: () -> Unit,
    onRefreshReminders: () -> Unit,
) {
    val original = rememberSaveable { current.urgentMinutes.toString() }
    val originalPolicy = rememberSaveable { current.expiredPolicy.name }
    val originalDaily = rememberSaveable { current.dailySummaryEnabled }
    val originalTime = rememberSaveable { String.format(java.util.Locale.ROOT, "%02d:%02d", current.dailySummaryMinuteOfDay / 60, current.dailySummaryMinuteOfDay % 60) }
    val originalDanger = rememberSaveable { current.dangerReminderEnabled }
    val originalDeadline = rememberSaveable { current.deadlineReminderEnabled }
    val originalLead = rememberSaveable { current.deadlineLeadMinutes.toString() }
    val originalStyle = rememberSaveable { current.characterStyle.name }
    var minutes by rememberSaveable { mutableStateOf(original) }
    var policy by rememberSaveable { mutableStateOf(originalPolicy) }
    var daily by rememberSaveable { mutableStateOf(originalDaily) }
    var summaryTime by rememberSaveable { mutableStateOf(originalTime) }
    var danger by rememberSaveable { mutableStateOf(originalDanger) }
    var deadline by rememberSaveable { mutableStateOf(originalDeadline) }
    var lead by rememberSaveable { mutableStateOf(originalLead) }
    var characterStyle by rememberSaveable { mutableStateOf(originalStyle) }
    var confirmArchive by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var discardDialog by rememberSaveable { mutableStateOf(false) }
    val parsed = minutes.trim().toLongOrNull()
    val parsedLead = lead.trim().toLongOrNull()
    val parsedTime = parseSummaryMinuteOfDay(summaryTime)
    val invalid = parsed == null || parsed <= 0 || parsed > SettingsMaxDurationMinutes
    val invalidLead = parsedLead == null || parsedLead <= 0 || parsedLead > SettingsMaxDurationMinutes
    val dirty = minutes != original || policy != originalPolicy || daily != originalDaily || summaryTime != originalTime ||
        danger != originalDanger || deadline != originalDeadline || lead != originalLead || characterStyle != originalStyle
    val back: () -> Unit = { if (!busy) { if (dirty) discardDialog = true else onBack() } }
    BackHandler(onBack = back)
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("应用设置", back, enabled = !busy)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            CharacterAppearancePicker(CharacterStyle.fromStored(characterStyle), enabled = !busy) {
                characterStyle = it.name
            }
            Surface(color = MaterialTheme.colorScheme.surface, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("普通紧急程度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("设置事项在截止前多久显示为橙色。进入速通危险期后，优先显示为红色。", color = Muted)
                    OutlinedTextField(minutes, { minutes = it }, enabled = !busy,
                        label = { Text("提前多少分钟") }, modifier = Modifier.fillMaxWidth().testTag("settings-urgent-minutes"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                        isError = submitted && invalid,
                        supportingText = { Text(if (submitted && invalid) "请输入有效的正整数分钟数。" else "默认 1440 分钟，即 24 小时。") })
                }
            }
            Text("通知提醒", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ReminderSwitch("每日 DDL 汇总", daily, busy, "settings-daily-enabled") { daily = it }
            OutlinedTextField(summaryTime, { summaryTime = it }, enabled = !busy,
                label = { Text("每日汇总时间（北京时间，HH:mm）") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("settings-summary-time"),
                isError = submitted && parsedTime == null,
                supportingText = { Text(if (submitted && parsedTime == null) "请输入 00:00 至 23:59 的时间。" else "默认 08:00，汇总今天截止的未完成、未归档事项；没有待办时不发送。") })
            ReminderSwitch("进入速通危险期时提醒", danger, busy, "settings-danger-enabled") { danger = it }
            ReminderSwitch("临近截止时提醒", deadline, busy, "settings-deadline-enabled") { deadline = it }
            OutlinedTextField(lead, { lead = it }, enabled = !busy,
                label = { Text("临近提醒提前分钟数") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("settings-deadline-lead"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = submitted && invalidLead,
                supportingText = { Text(if (submitted && invalidLead) "请输入有效的正整数分钟数。" else "默认提前 60 分钟通知，与事项变为橙色的时间分别设置。") })
            ReminderPermissionPanel(health, busy, onRefreshReminders)
            operationError?.let { ErrorNotice(it) }
            Text("过期事项处理", fontWeight = FontWeight.SemiBold)
            Column {
                FilterChip(policy == ExpiredPolicy.KEEP.name, { policy = ExpiredPolicy.KEEP.name },
                    enabled = !busy, label = { Text("灰显保留") }, modifier = Modifier.testTag("policy-keep"))
                FilterChip(policy == ExpiredPolicy.AUTO_ARCHIVE.name, { policy = ExpiredPolicy.AUTO_ARCHIVE.name },
                    enabled = !busy, label = { Text("自动归档过期未完成事项") }, modifier = Modifier.testTag("policy-auto"))
                Text("保存后生效。开启自动归档时，应用会在打开或使用期间整理过期未完成事项；归档后仍可恢复。", color = Muted,
                    style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = {
                submitted = true
                if (!invalid && !invalidLead && parsedTime != null) onSave(current.copy(
                    urgentMinutes = checkNotNull(parsed), expiredPolicy = ExpiredPolicy.valueOf(policy),
                    dailySummaryEnabled = daily, dailySummaryMinuteOfDay = parsedTime,
                    dangerReminderEnabled = danger, deadlineReminderEnabled = deadline,
                    deadlineLeadMinutes = checkNotNull(parsedLead),
                    characterStyle = CharacterStyle.fromStored(characterStyle),
                ))
            }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("settings-save")) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text("保存设置")
            }
            OutlinedButton(onCategories, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("manage-categories")) { Text("分类管理：来源与标签") }
            OutlinedButton({ confirmArchive = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("archive-expired")) { Text("一键归档过期事项") }
            Spacer(Modifier.height(8.dp))
            Text("时间与数据", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("所有时间均按北京时间显示。事项、图片和设置保存在本机，日常使用无需注册或联网。", color = Muted)
            Text("HUSTer's DDL Saver · 花园鳗", color = Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
    if (discardDialog) DiscardDialog({ discardDialog = false }, { discardDialog = false; onBack() })
    if (confirmArchive) AlertDialog(onDismissRequest = { confirmArchive = false },
        title = { Text("归档全部过期事项？") },
        text = { Text("将归档所有已过截止时间且尚未归档的事项，包括已完成事项。完成状态保留，之后可在“归档”中恢复。") },
        confirmButton = { TextButton({ confirmArchive = false; onArchiveExpired() }, enabled = !busy) { Text("确认归档") } },
        dismissButton = { TextButton({ confirmArchive = false }) { Text("取消") } })
}

@Composable
private fun ReminderSwitch(label: String, checked: Boolean, busy: Boolean, tag: String, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onChecked, enabled = !busy, modifier = Modifier.testTag(tag))
    }
}

@Composable
private fun ReminderPermissionPanel(health: ReminderHealth, busy: Boolean, onRefresh: () -> Unit) {
    val context = LocalContext.current
    var actionError by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onRefresh() }
    fun openSettings(intent: Intent) {
        try { context.startActivity(intent); actionError = null }
        catch (_: ActivityNotFoundException) { actionError = "无法打开系统设置，请在手机设置中查看本应用的通知与闹钟权限。" }
        catch (_: SecurityException) { actionError = "系统暂时不允许打开此设置，请从手机设置进入本应用。" }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (health.canPost) "系统通知：已允许" else "系统通知：未允许或提醒通道已关闭", modifier = Modifier.testTag("notification-permission-status"))
        Text(if (health.canScheduleExact) "提醒方式：可使用精确提醒" else "提醒方式：近似提醒，可能延迟",
            modifier = Modifier.testTag("exact-alarm-status"))
        ReminderStatusCopy.settingsPlansRetainedHint(health)?.let {
            Text(it, color = Muted, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("notification-plans-retained-hint"))
        }
        Text("省电和后台限制仍可能推迟提醒。手机重启或强制停止应用后，请先打开本应用检查提醒安排。", color = Muted,
            style = MaterialTheme.typography.bodySmall)
        Text("待发送提醒：${health.pendingCount}", color = Muted, modifier = Modifier.testTag("reminder-pending-count"))
        health.lastError?.let { ErrorNotice("提醒暂未安排完成，可重新检查并安排。详情：$it") }
        actionError?.let { ErrorNotice(it) }
        OutlinedButton({
            if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else openSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("request-notification-permission")) { Text("设置系统通知权限") }
        if (!health.canPost) {
            OutlinedButton({ openSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) },
                enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("open-notification-settings")) {
                Text("打开系统通知设置")
            }
        }
        if (Build.VERSION.SDK_INT >= 31 && !health.canScheduleExact) {
            OutlinedButton({ openSettings(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) },
                enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("request-exact-alarm")) { Text("允许精确提醒") }
        }
        TextButton(onRefresh, enabled = !busy, modifier = Modifier.testTag("refresh-reminders")) { Text("重新检查并安排提醒") }
    }
}
