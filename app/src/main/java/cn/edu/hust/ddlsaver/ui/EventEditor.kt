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
import cn.edu.hust.ddlsaver.domain.RecurrenceFrequency
import cn.edu.hust.ddlsaver.domain.RecurrenceSpec
import cn.edu.hust.ddlsaver.domain.Category
import cn.edu.hust.ddlsaver.domain.AttachmentRef
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
    var repeatName by rememberSaveable(session) { mutableStateOf("NONE") }
    var repeatEnd by rememberSaveable(session) { mutableStateOf("NONE") }
    var repeatCount by rememberSaveable(session) { mutableStateOf("") }
    var repeatUntil by rememberSaveable(session) { mutableStateOf<String?>(null) }
    var submitted by rememberSaveable(session) { mutableStateOf(false) }
    var discardDialog by rememberSaveable(session) { mutableStateOf(false) }
    val dirty = title != initialTitle || description != initialDescription || sprint != initialSprint ||
        importanceName != initialImportance || date != initialDate || time != initialTime ||
        sourceId != initialSource || tagsJson != initialTags || fieldsJson != initialFields || imagesJson != initialImages ||
        repeatName != "NONE" || repeatEnd != "NONE" || repeatCount.isNotEmpty() || repeatUntil != null
    val sprintValue = sprint.trim().toLongOrNull()
    val invalidSprint = sprint.isNotBlank() && (sprintValue == null || sprintValue <= 0L || sprintValue > MaxDurationMinutes)
    val invalidRepeat = repeatName != "NONE" && (images.isNotEmpty() ||
        (repeatEnd == "COUNT" && (repeatCount.toIntOrNull() == null || (repeatCount.toIntOrNull() ?: 0) <= 0)) ||
        (repeatEnd == "DATE" && (repeatUntil?.let(LocalDate::parse)?.let { end ->
            date?.let(LocalDate::parse)?.let { start -> end.isBefore(start) } ?: true
        } ?: true)))
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
            Text("填写标题和截止时间即可保存，其余信息按需补充。", color = Muted, style = MaterialTheme.typography.bodyMedium)
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
                    Text("按北京时间填写，精确到分钟。", color = Muted, style = MaterialTheme.typography.bodySmall)
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
                supportingText = { Text(if (submitted && invalidSprint) "请输入有效的正整数分钟数。" else "填写完成事项至少需要的分钟数；留空则不启用速通危险期。") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                isError = submitted && invalidSprint, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            )
            if (initial == null) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("重复", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("NONE" to "不重复", "DAILY" to "每天", "WEEKLY" to "每周", "MONTHLY" to "每月").forEach { (value, label) ->
                        FilterChip(repeatName == value, { repeatName = value }, enabled = !busy,
                            label = { Text(label) }, modifier = Modifier.testTag("repeat-$value"))
                    }
                }
                if (repeatName != "NONE") {
                    Text("重复事项不支持图片；编辑或删除某次事项只影响该次。", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("NONE" to "不结束", "COUNT" to "按次数", "DATE" to "按日期").forEach { (value, label) ->
                            FilterChip(repeatEnd == value, { repeatEnd = value }, enabled = !busy,
                                label = { Text(label) }, modifier = Modifier.testTag("repeat-end-$value"))
                        }
                    }
                    if (repeatEnd == "COUNT") OutlinedTextField(repeatCount, { repeatCount = it }, enabled = !busy,
                        label = { Text("重复次数（含首次）") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.testTag("repeat-count"))
                    if (repeatEnd == "DATE") OutlinedButton(onClick = {
                        val chosen = repeatUntil?.let(LocalDate::parse) ?: date?.let(LocalDate::parse) ?: now.atZone(BeijingZone).toLocalDate()
                        DatePickerDialog(context, { _, year, month, day -> repeatUntil = LocalDate.of(year, month + 1, day).toString() },
                            chosen.year, chosen.monthValue - 1, chosen.dayOfMonth).show()
                    }, enabled = !busy, modifier = Modifier.testTag("repeat-until")) { Text(repeatUntil ?: "选择结束日期（含当天）") }
                    if (submitted && invalidRepeat) Text("请检查结束日期、次数，且移除图片附件。", color = MaterialTheme.colorScheme.error)
                }
            }
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
                Text("用于标记事项的重要性，不影响按时间计算的紧急状态。", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(
                value = description, onValueChange = { description = it }, enabled = !busy,
                label = { Text("事项描述（选填）") }, placeholder = { Text("任务要求、提交方式或相关网址") },
                modifier = Modifier.fillMaxWidth().testTag("event-description"), minLines = 5, maxLines = 12, shape = RoundedCornerShape(16.dp),
            )
            EventCategories(sources, tags, sourceId, tagIds, initialSource, DraftJson.ids(initialTags), busy,
                onSource = { sourceId = it }, onTags = { tagsJson = DraftJson.ids(it) })
            CustomFieldEditor(fields, busy) { fieldsJson = DraftJson.fields(it) }
            if (initial?.recurrence == null) {
                AttachmentEditor(images, busy, onImportImages,
                    onRemove = { id -> imagesJson = DraftJson.attachments(images.filterNot { it.id == id }) },
                    onError = onImageError)
            } else Text("重复事项不支持图片附件。", color = Muted, style = MaterialTheme.typography.bodySmall)
            operationError?.let { ErrorNotice(it) }
            Button(
                onClick = {
                    submitted = true
                    if (title.isNotBlank() && date != null && time != null && !invalidSprint && !invalidRepeat) {
                        val deadline = LocalDate.parse(date).atTime(LocalTime.parse(time)).atZone(BeijingZone).toInstant().toEpochMilli()
                        onSave(
                            EventDraft(
                                id = initial?.id, title = title, deadlineAt = deadline,
                                sprintMinutes = if (sprint.isBlank()) null else sprintValue,
                                description = description, importance = Importance.valueOf(importanceName),
                                expectedRevision = expectedRevision,
                                sourceId = sourceId, tagIds = tagIds, customFields = fields, attachments = images,
                                recurrence = if (repeatName == "NONE") null else RecurrenceSpec(
                                    RecurrenceFrequency.valueOf(repeatName),
                                    untilDate = if (repeatEnd == "DATE") repeatUntil?.let(LocalDate::parse) else null,
                                    count = if (repeatEnd == "COUNT") repeatCount.toIntOrNull() else null),
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
internal fun DiscardDialog(onKeepEditing: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text("放弃尚未保存的修改？") },
        text = { Text("本次填写或修改的内容将不会保存。已保存的事项不受影响。") },
        confirmButton = { TextButton(onClick = onDiscard) { Text("放弃修改") } },
        dismissButton = { TextButton(onClick = onKeepEditing) { Text("继续编辑") } },
    )
}
