package cn.edu.hust.ddlsaver.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.EventDraft
import cn.edu.hust.ddlsaver.domain.Importance
import cn.edu.hust.ddlsaver.domain.Category
import cn.edu.hust.ddlsaver.domain.AttachmentRef
import cn.edu.hust.ddlsaver.domain.ExpiredPolicy
import android.net.Uri
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val MaxDurationMinutes = Long.MAX_VALUE / 60_000L

@Composable
internal fun EventEditor(
    session: Long,
    initial: DdlEvent?,
    now: Instant,
    busy: Boolean,
    operationError: String?,
    sources: List<Category>,
    tags: List<Category>,
    imageResult: ImageImportResult?,
    onImagesConsumed: (Long) -> Unit,
    onImportImages: (List<Uri>) -> Unit,
    onImageError: (String) -> Unit,
    onRegisterDraft: (List<AttachmentRef>, List<AttachmentRef>) -> Unit,
    onCancel: () -> Unit,
    onSave: (EventDraft) -> Unit,
) {
    val context = LocalContext.current
    // Freeze both initial values and the revision. New database emissions must not rewrite a draft.
    val initialDateTime = remember(session) { initial?.let { Instant.ofEpochMilli(it.deadlineAt).atZone(BeijingZone) } }
    val initialTitle = rememberSaveable(session) { initial?.title.orEmpty() }
    val initialDescription = rememberSaveable(session) { initial?.description.orEmpty() }
    val initialSprint = rememberSaveable(session) { initial?.sprintMinutes?.toString().orEmpty() }
    val initialImportance = rememberSaveable(session) { (initial?.importance ?: Importance.NORMAL).name }
    val initialDate by rememberSaveable(session) { mutableStateOf<String?>(initialDateTime?.toLocalDate()?.toString()) }
    val initialTime by rememberSaveable(session) {
        mutableStateOf<String?>(initialDateTime?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")))
    }
    val expectedRevision by rememberSaveable(session) { mutableStateOf<Long?>(initial?.revision) }
    val initialSource by rememberSaveable(session) { mutableStateOf(initial?.source?.id) }
    val initialTags = rememberSaveable(session) { DraftJson.ids(initial?.tags.orEmpty().map { it.id }) }
    val initialFields = rememberSaveable(session) { DraftJson.fields(initial?.customFields.orEmpty()) }
    val initialImages = rememberSaveable(session) { DraftJson.attachments(initial?.attachments.orEmpty()) }
    var sourceId by rememberSaveable(session) { mutableStateOf(initialSource) }
    var tagsJson by rememberSaveable(session) { mutableStateOf(initialTags) }
    var fieldsJson by rememberSaveable(session) { mutableStateOf(initialFields) }
    var imagesJson by rememberSaveable(session) { mutableStateOf(initialImages) }
    val tagIds = remember(tagsJson) { DraftJson.ids(tagsJson) }
    val fields = remember(fieldsJson) { DraftJson.fields(fieldsJson) }
    val images = remember(imagesJson) { DraftJson.attachments(imagesJson) }
    val originalImages = remember(initialImages) { DraftJson.attachments(initialImages) }
    SideEffect { onRegisterDraft(originalImages, images) }
    LaunchedEffect(imageResult?.token) {
        imageResult?.takeIf { it.session == session }?.let { result ->
            imagesJson = DraftJson.attachments((images + result.attachments).distinctBy { it.id })
            onImagesConsumed(result.token)
        }
    }
    var title by rememberSaveable(session) { mutableStateOf(initialTitle) }
    var description by rememberSaveable(session) { mutableStateOf(initialDescription) }
    var sprint by rememberSaveable(session) { mutableStateOf(initialSprint) }
    var importanceName by rememberSaveable(session) { mutableStateOf(initialImportance) }
    var date by rememberSaveable(session) { mutableStateOf(initialDate) }
    var time by rememberSaveable(session) { mutableStateOf(initialTime) }
    var submitted by rememberSaveable(session) { mutableStateOf(false) }
    var discardDialog by rememberSaveable(session) { mutableStateOf(false) }
    val dirty = title != initialTitle || description != initialDescription || sprint != initialSprint ||
        importanceName != initialImportance || date != initialDate || time != initialTime ||
        sourceId != initialSource || tagsJson != initialTags || fieldsJson != initialFields || imagesJson != initialImages
    val sprintValue = sprint.trim().toLongOrNull()
    val invalidSprint = sprint.isNotBlank() && (sprintValue == null || sprintValue <= 0L || sprintValue > MaxDurationMinutes)
    val back: () -> Unit = {
        if (!busy) {
            if (dirty) discardDialog = true else onCancel()
        }
    }
    BackHandler(onBack = back)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(if (initial == null) "新建事项" else "编辑事项", back, enabled = !busy)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("先记下截止时间，再从容安排。", color = Muted, style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = title, onValueChange = { title = it }, enabled = !busy,
                label = { Text("事项标题 *") }, placeholder = { Text("例如：提交软件工程实验报告") },
                modifier = Modifier.fillMaxWidth().testTag("event-title"), shape = RoundedCornerShape(16.dp),
                isError = submitted && title.isBlank(), minLines = 1, maxLines = 4,
                supportingText = if (submitted && title.isBlank()) { { Text("请填写事项标题。") } } else null,
            )
            Surface(color = Color.White, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("截止时间 *", fontWeight = FontWeight.SemiBold)
                    Text("以下日期和时间均为北京时间。", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                val selected = date?.let(LocalDate::parse) ?: now.atZone(BeijingZone).toLocalDate()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day -> date = LocalDate.of(year, month + 1, day).toString() },
                                    selected.year, selected.monthValue - 1, selected.dayOfMonth,
                                ).show()
                            },
                            enabled = !busy, modifier = Modifier.weight(1f),
                        ) { Text(date ?: "选择日期") }
                        OutlinedButton(
                            onClick = {
                                val selected = time?.let(LocalTime::parse) ?: now.atZone(BeijingZone).toLocalTime()
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute -> time = LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern("HH:mm")) },
                                    selected.hour, selected.minute, true,
                                ).show()
                            },
                            enabled = !busy, modifier = Modifier.weight(1f),
                        ) { Text(time ?: "选择时间") }
                    }
                    if (submitted && (date == null || time == null)) {
                        Text("请选择完整的截止日期和时间。", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            OutlinedTextField(
                value = sprint, onValueChange = { sprint = it }, enabled = !busy,
                label = { Text("速通时间（分钟，选填）") },
                supportingText = { Text(if (submitted && invalidSprint) "请输入有效的正整数分钟数。" else "估计完成这件事最少需要多久。留空则不计算危险期。") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                isError = submitted && invalidSprint, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            )
            Column {
                Text("重要程度", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Importance.entries.forEach { item ->
                        FilterChip(
                            selected = importanceName == item.name,
                            onClick = { importanceName = item.name }, enabled = !busy,
                            label = { Text(importanceLabel(item)) },
                        )
                    }
                }
                Text("重要程度不改变按时间计算的紧急状态。", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(
                value = description, onValueChange = { description = it }, enabled = !busy,
                label = { Text("事项描述（选填）") }, placeholder = { Text("任务要求、提交方式或相关网址") },
                modifier = Modifier.fillMaxWidth().testTag("event-description"), minLines = 5, maxLines = 12, shape = RoundedCornerShape(16.dp),
            )
            EventCategories(sources, tags, sourceId, tagIds, initialSource, DraftJson.ids(initialTags), busy,
                onSource = { sourceId = it }, onTags = { tagsJson = DraftJson.ids(it) })
            CustomFieldEditor(fields, busy) { fieldsJson = DraftJson.fields(it) }
            AttachmentEditor(images, busy, onImportImages,
                onRemove = { id -> imagesJson = DraftJson.attachments(images.filterNot { it.id == id }) },
                onError = onImageError)
            operationError?.let { ErrorNotice(it) }
            Button(
                onClick = {
                    submitted = true
                    if (title.isNotBlank() && date != null && time != null && !invalidSprint) {
                        val deadline = LocalDate.parse(date).atTime(LocalTime.parse(time)).atZone(BeijingZone).toInstant().toEpochMilli()
                        onSave(
                            EventDraft(
                                id = initial?.id, title = title, deadlineAt = deadline,
                                sprintMinutes = if (sprint.isBlank()) null else sprintValue,
                                description = description, importance = Importance.valueOf(importanceName),
                                expectedRevision = expectedRevision,
                                sourceId = sourceId, tagIds = tagIds, customFields = fields, attachments = images,
                            ),
                        )
                    }
                },
                enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("event-save"),
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text("保存事项")
            }
            TextButton(onClick = back, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("取消") }
        }
    }
    if (discardDialog) {
        DiscardDialog(onKeepEditing = { discardDialog = false }, onDiscard = { discardDialog = false; onCancel() })
    }
}

@Composable
internal fun SettingsScreen(
    currentMinutes: Long,
    currentPolicy: ExpiredPolicy,
    busy: Boolean,
    operationError: String?,
    onBack: () -> Unit,
    onSave: (Long, ExpiredPolicy) -> Unit,
    onCategories: () -> Unit,
    onArchiveExpired: () -> Unit,
) {
    val original = rememberSaveable { currentMinutes.toString() }
    var minutes by rememberSaveable { mutableStateOf(original) }
    val originalPolicy = rememberSaveable { currentPolicy.name }
    var policy by rememberSaveable { mutableStateOf(originalPolicy) }
    var confirmArchive by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var discardDialog by rememberSaveable { mutableStateOf(false) }
    val parsed = minutes.trim().toLongOrNull()
    val invalid = parsed == null || parsed <= 0 || parsed > MaxDurationMinutes
    val back: () -> Unit = {
        if (!busy) {
            if (minutes != original || policy != originalPolicy) discardDialog = true else onBack()
        }
    }
    BackHandler(onBack = back)
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("应用设置", back, enabled = !busy)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("普通紧急程度", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("距离截止时间进入这个范围时，事项显示为橙色。速通危险期仍使用每件事项自己的速通时间。", color = Muted)
                    OutlinedTextField(
                        value = minutes, onValueChange = { minutes = it }, enabled = !busy,
                        label = { Text("提前多少分钟") }, modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                        isError = submitted && invalid,
                        supportingText = { Text(if (submitted && invalid) "请输入有效的正整数分钟数。" else "默认 1440 分钟，即 24 小时。") },
                    )
                }
            }
            operationError?.let { ErrorNotice(it) }
            Text("过期事项处理", fontWeight = FontWeight.SemiBold)
            Column {
                FilterChip(policy == ExpiredPolicy.KEEP.name, { policy = ExpiredPolicy.KEEP.name },
                    enabled = !busy, label = { Text("灰显保留") }, modifier = Modifier.testTag("policy-keep"))
                FilterChip(policy == ExpiredPolicy.AUTO_ARCHIVE.name, { policy = ExpiredPolicy.AUTO_ARCHIVE.name },
                    enabled = !busy, label = { Text("自动归档过期未完成事项") }, modifier = Modifier.testTag("policy-auto"))
                Text("保存设置后立即生效，并在进入应用及分钟刷新时检查。归档可恢复。", color = Muted,
                    style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { submitted = true; if (!invalid) onSave(checkNotNull(parsed), ExpiredPolicy.valueOf(policy)) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Text("保存设置")
            }
            OutlinedButton(onClick = onCategories, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("manage-categories")) { Text("分类管理：来源与标签") }
            OutlinedButton(onClick = { confirmArchive = true }, enabled = !busy, modifier = Modifier.fillMaxWidth().testTag("archive-expired")) { Text("一键归档过期事项") }
            Spacer(Modifier.height(8.dp))
            Text("时间与数据", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("所有事项按北京时间展示。事项和设置保存在本机，无需注册即可使用。", color = Muted, style = MaterialTheme.typography.bodyMedium)
            Text("HUSTer's DDL Saver · 花园鳗", color = Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
    if (discardDialog) {
        DiscardDialog(onKeepEditing = { discardDialog = false }, onDiscard = { discardDialog = false; onBack() })
    }
    if (confirmArchive) AlertDialog(
        onDismissRequest = { confirmArchive = false }, title = { Text("归档全部过期事项？") },
        text = { Text("所有已过截止时间且未归档的事项将移入归档列表，包括已完成事项。完成标记保留，之后可以恢复归档。") },
        confirmButton = { TextButton({ confirmArchive = false; onArchiveExpired() }) { Text("确认归档") } },
        dismissButton = { TextButton({ confirmArchive = false }) { Text("取消") } },
    )
}

@Composable
private fun DiscardDialog(onKeepEditing: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text("放弃尚未保存的修改？") },
        text = { Text("返回后，本次填写或修改的内容不会保存。") },
        confirmButton = { TextButton(onClick = onDiscard) { Text("放弃修改") } },
        dismissButton = { TextButton(onClick = onKeepEditing) { Text("继续编辑") } },
    )
}
