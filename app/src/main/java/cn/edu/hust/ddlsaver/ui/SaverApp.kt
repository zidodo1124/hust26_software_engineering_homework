package cn.edu.hust.ddlsaver.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.Importance
import cn.edu.hust.ddlsaver.domain.Urgency
import cn.edu.hust.ddlsaver.domain.UrgencyRules
import cn.edu.hust.ddlsaver.domain.selectCharacterFeedback
import cn.edu.hust.ddlsaver.reminders.ReminderStatusCopy
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

internal val BeijingZone: ZoneId = ZoneId.of("Asia/Shanghai")
private val DeadlineFormat = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA)
    .withZone(BeijingZone)
private val ShortDeadlineFormat = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)
    .withZone(BeijingZone)

internal fun formatDeadline(value: Long): String = DeadlineFormat.format(Instant.ofEpochMilli(value))

@Composable
fun SaverApp(model: SaverViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val reminderHealth by model.reminderHealth.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf("home") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorSession by rememberSaveable { mutableLongStateOf(0L) }
    var batchToken by rememberSaveable { mutableLongStateOf(0L) }
    var categoryToken by rememberSaveable { mutableLongStateOf(0L) }
    var homeGeneration by rememberSaveable { mutableLongStateOf(0L) }
    var todayOnHome by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val homeState = rememberSaveableStateHolder()
    val owner = LocalLifecycleOwner.current
    val working = state.busy || state.actionResult != null || state.imageImportResult != null

    fun openReminder(request: ReminderNavigation) {
        homeState.removeState("home-$homeGeneration")
        homeGeneration++
        todayOnHome = request.today
        model.clearOperationError()
        val target = request.eventId?.let { id -> state.events.firstOrNull { it.id == id } }
        if (target != null) {
            selectedId = target.id
            screen = "detail"
        } else {
            selectedId = null
            screen = "home"
            if (request.eventId != null) snackbarScope.launch {
                snackbar.showSnackbar("这条提醒里的事项已经不在了。", withDismissAction = true)
            }
        }
        model.acknowledgeReminderNavigation(request.token)
    }
    val editingScreen = screen == "editor" || screen == "settings" || screen == "categories"
    LaunchedEffect(state.reminderNavigation, state.loading, working, screen, state.events) {
        val request = state.reminderNavigation ?: return@LaunchedEffect
        if (!state.loading && state.loadError == null && !working && !editingScreen) openReminder(request)
    }

    LaunchedEffect(state.loading, screen) {
        if (!state.loading && screen != "editor") model.cleanupOldAttachments()
    }
    LaunchedEffect(state.cleanupWarning) {
        state.cleanupWarning?.let { warning ->
            model.acknowledgeCleanupWarning()
            snackbarScope.launch { snackbar.showSnackbar(warning, withDismissAction = true) }
        }
    }

    DisposableEffect(owner, model) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) model.refreshTime()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(state.actionResult, state.events) {
        val result = state.actionResult ?: return@LaunchedEffect
        // Room's observer may emit after save() returns; wait before opening the new detail.
        if (result.kind == "save" && state.events.none {
            it.id == result.eventId && it.revision >= (result.savedRevision ?: 0L)
        }) return@LaunchedEffect
        when (result.kind) {
            "batch" -> batchToken = result.token
            "category" -> categoryToken = result.token
            "save" -> {
                homeState.removeState("editor-$editorSession")
                selectedId = result.eventId
                screen = "detail"
            }
            "delete", "settings" -> {
                if (result.kind == "settings") homeState.removeState("settings")
                screen = "home"
                selectedId = null
            }
        }
        model.acknowledgeAction(result.token)
        snackbarScope.launch {
            snackbar.showSnackbar(
                message = when (result.kind) {
                    "save" -> "事项已保存"
                    "delete" -> "事项已永久删除"
                    "settings" -> "设置已保存"
                    "batch" -> "批量操作已完成"
                    "category" -> "分类已更新"
                    else -> "事项状态已更新"
                },
                withDismissAction = true,
            )
        }
    }
    BackHandler(enabled = screen == "detail") { if (!working) screen = "home" }

    Scaffold(
        containerColor = Paper,
        // Reserve measured space for feedback instead of covering scrolled form/action buttons.
        bottomBar = { SnackbarHost(snackbar, Modifier.navigationBarsPadding()) },
        floatingActionButton = {
            if (screen == "home" && !state.loading && state.loadError == null) {
                ExtendedFloatingActionButton(
                    modifier = Modifier.testTag("create-event"),
                    onClick = {
                        if (!working) {
                        selectedId = null
                        homeState.removeState("editor-$editorSession")
                        editorSession++
                        model.clearOperationError()
                        screen = "editor"
                        }
                    },
                    containerColor = Ocean,
                    contentColor = Color.White,
                ) { Text("＋  新建事项", fontWeight = FontWeight.SemiBold) }
            }
        },
    ) { inset ->
        Box(Modifier.fillMaxSize().padding(inset).consumeWindowInsets(inset).imePadding()) {
            when {
                state.loading -> LoadingContent()
                state.loadError != null -> StatusContent(
                    title = "事项暂时打不开",
                    message = state.loadError.orEmpty(),
                    actionLabel = "重新加载",
                    onAction = model::reload,
                )
                screen == "home" -> homeState.SaveableStateProvider("home-$homeGeneration") {
                    HomeScreen(
                        state,
                        batchToken = batchToken,
                        initiallyToday = todayOnHome,
                        reminderWarning = ReminderStatusCopy.homeWarning(state.settings, reminderHealth),
                        onSettings = { model.clearOperationError(); screen = "settings" },
                        onOpen = { selectedId = it; model.clearOperationError(); screen = "detail" },
                        onBatch = model::applyBatch,
                    )
                }
                screen == "settings" -> homeState.SaveableStateProvider("settings") {
                    SettingsScreen(
                        current = state.settings,
                        health = reminderHealth,
                        busy = working,
                        operationError = state.operationError,
                        onBack = {
                            homeState.removeState("settings")
                            model.clearOperationError()
                            screen = "home"
                        },
                        onSave = model::saveSettings,
                        onCategories = { model.clearOperationError(); screen = "categories" },
                        onArchiveExpired = model::archiveExpired,
                        onRefreshReminders = model::refreshReminders,
                    )
                }
                screen == "categories" -> CategoryManager(
                    sources = state.sources, tags = state.tags, busy = working,
                    operationError = state.operationError, successToken = categoryToken,
                    onBack = { model.clearOperationError(); screen = "settings" },
                    onSave = model::saveCategory, onToggle = model::setCategoryActive,
                    onClearError = model::clearOperationError,
                )
                else -> {
                    val selected = state.events.firstOrNull { it.id == selectedId }
                    if (selectedId != null && selected == null) {
                        StatusContent("事项已不存在", "这件事项可能已经删了，回列表看看其他事项吧。", "返回列表") {
                            selectedId = null; screen = "home"
                        }
                    } else if (screen == "editor") {
                        homeState.SaveableStateProvider("editor-$editorSession") {
                            EventEditor(
                                session = editorSession,
                                initial = selected,
                                now = state.now,
                                busy = working,
                                operationError = state.operationError,
                                sources = state.sources, tags = state.tags,
                                imageResult = state.imageImportResult,
                                onImagesConsumed = model::acknowledgeImages,
                                onImportImages = { model.importImages(editorSession, it) },
                                onImageError = model::reportError,
                                onRegisterDraft = { originals, current ->
                                    model.registerDraft(editorSession, originals, current)
                                    model.cleanupOldAttachments()
                                },
                                onCancel = {
                                    model.discardDraft(editorSession)
                                    homeState.removeState("editor-$editorSession")
                                    model.clearOperationError()
                                    screen = if (selectedId == null) "home" else "detail"
                                },
                                onSave = { model.save(it, editorSession) },
                            )
                        }
                    } else if (selected != null) {
                        DetailScreen(
                            event = selected,
                            state = state,
                            busy = working,
                            onBack = { model.clearOperationError(); screen = "home" },
                            onEdit = {
                                homeState.removeState("editor-$editorSession")
                                editorSession++
                                model.clearOperationError()
                                screen = "editor"
                            },
                            onComplete = { model.setCompleted(selected) },
                            onArchive = { model.setArchived(selected) },
                            onDelete = { model.delete(selected) },
                            onDeleteFollowing = { model.deleteFollowing(selected) },
                        )
                    } else {
                        StatusContent("选择一件事项", "回首页选一件，或点“新建事项”记下来。", "返回首页") {
                            screen = "home"
                        }
                    }
                }
            }
        }
    }
    state.reminderNavigation?.let { request ->
        if (!state.loading && !working && editingScreen && !request.deferred) {
            AlertDialog(
                onDismissRequest = { model.deferReminderNavigation(request.token) },
                title = { Text("查看通知中的事项？") },
                text = { Text("当前修改可能还没保存。点“稍后查看”继续编辑；点“放弃修改并打开”会丢弃这些修改，打开通知里的事项。") },
                dismissButton = { TextButton({ model.deferReminderNavigation(request.token) }, modifier = Modifier.testTag("reminder-defer")) { Text("稍后查看") } },
                confirmButton = { TextButton({
                    if (screen == "editor") {
                        model.discardDraft(editorSession)
                        homeState.removeState("editor-$editorSession")
                    }
                    if (screen == "settings" || screen == "categories") homeState.removeState("settings")
                    openReminder(request)
                }, modifier = Modifier.testTag("reminder-discard-open")) { Text("放弃修改并打开") } },
            )
        }
    }
}

@Composable
internal fun CountTile(label: String, count: Int, modifier: Modifier, tint: Color, onClick: () -> Unit) {
    Surface(modifier = modifier, onClick = onClick, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(count.toString(), color = tint, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(label, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun EventCard(event: DdlEvent, state: SaverUiState, onOpen: (String) -> Unit) {
    val urgency = UrgencyRules.classify(event, state.now, state.settings.urgentMinutes)
    val colors = urgencyColors(urgency)
    Card(
        modifier = Modifier.testTag("event-card-${event.id}"),
        onClick = { onOpen(event.id) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (urgency == Urgency.OVERDUE) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (urgency == Urgency.DANGER) Color(0xFFECC0C1) else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(urgency)
                Spacer(Modifier.weight(1f))
                Text(importanceLabel(event.importance), color = Muted, style = MaterialTheme.typography.labelSmall)
            }
            Text(event.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val classification = listOfNotNull(event.source?.name) + event.tags.map { "#${it.name}" }
            if (classification.isNotEmpty()) {
                Text(classification.joinToString(" · "), color = Ocean, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(colors.first, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(ShortDeadlineFormat.format(Instant.ofEpochMilli(event.deadlineAt)) + " 截止", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
            if (!event.description.isBlank()) {
                Text(event.description, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            if (event.sprintMinutes != null) {
                Text("速通时间 ${formatDuration(event.sprintMinutes)}", color = colors.first, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun DetailScreen(
    event: DdlEvent, state: SaverUiState, busy: Boolean, onBack: () -> Unit,
    onEdit: () -> Unit, onComplete: () -> Unit, onArchive: () -> Unit, onDelete: () -> Unit,
    onDeleteFollowing: () -> Unit,
) {
    var confirmDelete by rememberSaveable(event.id) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    var copied by rememberSaveable(event.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("事项详情", onBack, enabled = !busy)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(color = Color.White, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatusBadge(UrgencyRules.classify(event, state.now, state.settings.urgentMinutes))
                    SelectionContainer { Text(event.title, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
                    Text("截止时间 · 北京时间", color = Muted, style = MaterialTheme.typography.labelMedium)
                    Text(formatDeadline(event.deadlineAt), style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailLine("重要程度", importanceLabel(event.importance))
                    DetailLine("速通时间", event.sprintMinutes?.let(::formatDuration) ?: "未填写")
                    DetailLine("完成状态", if (event.completedAt == null) "未完成" else "已完成")
                    if (event.recurrence != null) DetailLine("重复事项", "第 ${event.recurrence.index + 1} 次；编辑或删除时可选择范围")
                    DetailLine("归档状态", if (event.archivedAt == null) "未归档" else "已归档")
                    DetailLine("来源", event.source?.let { it.name + if (it.isActive) "" else "（已停用）" } ?: "未指定")
                    DetailLine("标签", event.tags.joinToString("、") { it.name + if (it.isActive) "" else "（已停用）" }.ifEmpty { "未指定" })
                }
            }
            Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("事项描述", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        if (event.description.isNotEmpty()) TextButton(onClick = {
                            clipboard.setText(AnnotatedString(event.description)); copied = true
                        }) { Text(if (copied) "已复制" else "复制全文") }
                    }
                    SelectionContainer {
                        Text(event.description.ifEmpty { "还没写描述，点“编辑事项”可以补上。" }, color = if (event.description.isEmpty()) Muted else Ink)
                    }
                }
            }
            if (event.customFields.isNotEmpty()) Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("自定义字段", fontWeight = FontWeight.SemiBold)
                    event.customFields.forEach { field ->
                        Column {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(field.label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                TextButton({ clipboard.setText(AnnotatedString(field.value)) }, modifier = Modifier.testTag("copy-field-${field.id}")) { Text("复制") }
                            }
                            SelectionContainer { Text(field.value, modifier = Modifier.testTag("detail-field-${field.id}")) }
                        }
                    }
                }
            }
            if (event.attachments.isNotEmpty()) AttachmentGallery(event.attachments)
            state.operationError?.let { ErrorNotice(it) }
            if (state.justCompleted) CharacterPanel(selectCharacterFeedback(state.events, state.now, state.settings, justCompleted = true),
                style = state.settings.characterStyle)
            Button(onClick = onComplete, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (event.completedAt == null) "标记完成" else "恢复为未完成")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onEdit, enabled = !busy, modifier = Modifier.weight(1f)) { Text("编辑事项") }
                OutlinedButton(onClick = onArchive, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text(if (event.archivedAt == null) "归档" else "移出归档")
                }
            }
            TextButton(onClick = { confirmDelete = true }, enabled = !busy, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("永久删除", color = MaterialTheme.colorScheme.error)
            }
            if (busy) CircularProgressIndicator(Modifier.size(24.dp).align(Alignment.CenterHorizontally))
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!busy) confirmDelete = false },
            title = { Text("永久删除这件事项？") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(event.title, maxLines = 4, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(if (event.recurrence == null) "这件事项和 App 内的附件副本会永久删除，无法恢复。相册原图不受影响。"
                        else if (event.completedAt != null || event.archivedAt != null) "只删除这一次，不影响其他重复事项。删除后无法恢复。" else "只删本次不会影响其他周期；删除本次及后续会停止重复，保留已完成或归档记录。删除后无法恢复。")
                }
            },
            confirmButton = {
                Column {
                    TextButton(onClick = { confirmDelete = false; onDelete() }, enabled = !busy) {
                        Text(if (event.recurrence == null) "确认删除" else "仅删除本次", color = MaterialTheme.colorScheme.error)
                    }
                    if (event.recurrence != null && event.completedAt == null && event.archivedAt == null) {
                        TextButton(onClick = { confirmDelete = false; onDeleteFollowing() }, enabled = !busy,
                            modifier = Modifier.testTag("delete-this-and-future")) { Text("删除本次及后续", color = MaterialTheme.colorScheme.error) }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }, enabled = !busy) { Text("取消") } },
        )
    }
}

@Composable
internal fun ScreenHeader(title: String, onBack: () -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 4.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack, enabled = enabled) { Text("‹ 返回") }
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = Muted, modifier = Modifier.width(76.dp), style = MaterialTheme.typography.bodyMedium)
        Text(value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun ErrorNotice(message: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(14.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun LoadingContent() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("正在加载事项…", color = Muted)
    }
}

@Composable
private fun StatusContent(title: String, message: String, actionLabel: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, color = Muted, modifier = Modifier.padding(vertical = 16.dp))
        Button(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun StatusBadge(urgency: Urgency) {
    val (foreground, background) = urgencyColors(urgency)
    Surface(color = background, shape = RoundedCornerShape(8.dp)) {
        Text(
            when (urgency) {
                Urgency.NORMAL -> "时间充裕"
                Urgency.SOON -> "临近截止"
                Urgency.DANGER -> "! 速通危险期"
                Urgency.OVERDUE -> "已逾期"
                Urgency.COMPLETED -> "已完成"
                Urgency.ARCHIVED -> "已归档"
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = foreground, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
        )
    }
}

internal fun urgencyColors(urgency: Urgency): Pair<Color, Color> = when (urgency) {
    Urgency.DANGER -> Color(0xFFA12435) to Color(0xFFFFE6E9)
    Urgency.SOON -> Color(0xFF8C4A0B) to Color(0xFFFFEAD2)
    Urgency.COMPLETED -> Color(0xFF216549) to Color(0xFFDFF1E7)
    Urgency.ARCHIVED, Urgency.OVERDUE -> Color(0xFF536375) to Color(0xFFDCE3EB)
    Urgency.NORMAL -> Ocean to Color(0xFFDDF0F5)
}

internal fun importanceLabel(importance: Importance): String = when (importance) {
    Importance.LOW -> "低重要度"
    Importance.NORMAL -> "中重要度"
    Importance.HIGH -> "高重要度"
}

internal fun formatDuration(minutes: Long): String = when {
    minutes < 60 -> "$minutes 分钟"
    minutes % 60L == 0L -> "${minutes / 60} 小时"
    else -> "${minutes / 60} 小时 ${minutes % 60} 分钟"
}
