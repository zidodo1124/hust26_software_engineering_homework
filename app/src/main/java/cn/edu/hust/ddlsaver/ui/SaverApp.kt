package cn.edu.hust.ddlsaver.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
    var screen by rememberSaveable { mutableStateOf("home") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorSession by rememberSaveable { mutableLongStateOf(0L) }
    val snackbar = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val homeState = rememberSaveableStateHolder()
    val owner = LocalLifecycleOwner.current
    val working = state.busy || state.actionResult != null

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
                    "delete" -> "事项已删除"
                    "settings" -> "设置已保存"
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
                        selectedId = null
                        homeState.removeState("editor-$editorSession")
                        editorSession++
                        model.clearOperationError()
                        screen = "editor"
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
                    title = "暂时无法打开事项",
                    message = state.loadError.orEmpty(),
                    actionLabel = "重新读取",
                    onAction = model::reload,
                )
                screen == "home" -> homeState.SaveableStateProvider("home") {
                    HomeScreen(
                        state,
                        onSettings = { model.clearOperationError(); screen = "settings" },
                        onOpen = { selectedId = it; model.clearOperationError(); screen = "detail" },
                    )
                }
                screen == "settings" -> homeState.SaveableStateProvider("settings") {
                    SettingsScreen(
                        currentMinutes = state.settings.urgentMinutes,
                        busy = working,
                        operationError = state.operationError,
                        onBack = {
                            homeState.removeState("settings")
                            model.clearOperationError()
                            screen = "home"
                        },
                        onSave = model::saveUrgentMinutes,
                    )
                }
                else -> {
                    val selected = state.events.firstOrNull { it.id == selectedId }
                    if (selectedId != null && selected == null) {
                        StatusContent("事项已不存在", "它可能已经被删除。", "返回列表") {
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
                                onCancel = {
                                    homeState.removeState("editor-$editorSession")
                                    model.clearOperationError()
                                    screen = if (selectedId == null) "home" else "detail"
                                },
                                onSave = model::save,
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
                        )
                    } else {
                        StatusContent("选择一件事项", "从首页查看或创建你的 DDL。", "返回首页") {
                            screen = "home"
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(state: SaverUiState, onSettings: () -> Unit, onOpen: (String) -> Unit) {
    var mode by rememberSaveable { mutableStateOf("todo") }
    var quickFilter by rememberSaveable { mutableStateOf("all") }
    var query by rememberSaveable { mutableStateOf("") }
    val active = state.events.filter(UrgencyRules::isActive)
    val todayCount = active.count { UrgencyRules.isToday(it, state.now) }
    val dangerCount = active.count {
        UrgencyRules.classify(it, state.now, state.settings.urgentMinutes) == Urgency.DANGER
    }
    val filtered = state.events.filter { event ->
        val inMode = when (mode) {
            "done" -> event.completedAt != null && event.archivedAt == null
            "archive" -> event.archivedAt != null
            else -> UrgencyRules.isActive(event)
        }
        val inQuickFilter = mode != "todo" || when (quickFilter) {
            "today" -> UrgencyRules.isToday(event, state.now)
            "danger" -> UrgencyRules.classify(event, state.now, state.settings.urgentMinutes) == Urgency.DANGER
            else -> true
        }
        val keyword = query.trim()
        inMode && inQuickFilter && (keyword.isEmpty() ||
            event.title.contains(keyword, ignoreCase = true) || event.description.contains(keyword, ignoreCase = true))
    }.sortedWith(compareBy<DdlEvent> { it.deadlineAt }.thenBy { it.id })
    val overdue = if (mode == "todo") filtered.filter { it.deadlineAt <= state.now.toEpochMilli() } else emptyList()
    val current = if (mode == "todo") filtered.filter { it.deadlineAt > state.now.toEpochMilli() } else filtered

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("event-list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("HUSTer's", color = Muted, style = MaterialTheme.typography.labelLarge)
                    Text("DDL Saver", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onSettings) { Text("设置") }
            }
        }
        item {
            Surface(color = Ink, shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("把重要的事，安排妥当。", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        DateTimeFormatter.ofPattern("M月d日 EEEE  HH:mm", Locale.CHINA).withZone(BeijingZone).format(state.now) + " · 北京时间",
                        color = Color(0xFFCBDCEB), style = MaterialTheme.typography.bodySmall,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CountTile("今日 DDL", todayCount, Modifier.weight(1f), Color(0xFFD6EEF6)) {
                            mode = "todo"; quickFilter = "today"; query = ""
                        }
                        CountTile("速通危险期", dangerCount, Modifier.weight(1f), Color(0xFFFFD7CE)) {
                            mode = "todo"; quickFilter = "danger"; query = ""
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("todo" to "待办", "done" to "已完成", "archive" to "归档").forEach { (key, label) ->
                    FilterChip(selected = mode == key, onClick = { mode = key; quickFilter = "all" }, label = { Text(label) })
                }
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                label = { Text("搜索标题或描述") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = if (query.isNotEmpty()) { { TextButton(onClick = { query = "" }) { Text("清除") } } } else null,
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        mode == "done" -> "已完成事项"
                        mode == "archive" -> "归档事项"
                        quickFilter == "today" -> "今日待办"
                        quickFilter == "danger" -> "危险期待办"
                        else -> "全部待办"
                    } + " · ${filtered.size}",
                    modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold,
                )
                if (mode == "todo" && quickFilter != "all") {
                    TextButton(onClick = { quickFilter = "all" }) { Text("查看全部") }
                } else {
                    Text("按截止时间", color = Muted, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (query.isNotBlank()) "没有找到相关事项" else "这里暂时没有事项", fontWeight = FontWeight.SemiBold)
                        Text(
                            if (query.isNotBlank()) "试试其他关键词，或清除搜索条件。"
                            else if (mode == "todo" && quickFilter == "all") "点击“新建事项”，记录下一件需要完成的事。"
                            else "符合当前条件的事项会显示在这里。",
                            color = Muted, style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        items(current, key = { it.id }) { event -> EventCard(event, state, onOpen) }
        if (overdue.isNotEmpty()) {
            item { Text("已逾期 · ${overdue.size}", color = Muted, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
            items(overdue, key = { it.id }) { event -> EventCard(event, state, onOpen) }
        }
    }
}

@Composable
private fun CountTile(label: String, count: Int, modifier: Modifier, tint: Color, onClick: () -> Unit) {
    Surface(modifier = modifier, onClick = onClick, color = Color(0xFF29415C), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(count.toString(), color = tint, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun EventCard(event: DdlEvent, state: SaverUiState, onOpen: (String) -> Unit) {
    val urgency = UrgencyRules.classify(event, state.now, state.settings.urgentMinutes)
    val colors = urgencyColors(urgency)
    Card(
        onClick = { onOpen(event.id) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (urgency == Urgency.OVERDUE) Color(0xFFEBEFF4) else Color.White),
        border = BorderStroke(1.dp, if (urgency == Urgency.DANGER) Color(0xFFECC0C1) else Color(0xFFDCE4ED)),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(urgency)
                Spacer(Modifier.weight(1f))
                Text(importanceLabel(event.importance), color = Muted, style = MaterialTheme.typography.labelSmall)
            }
            Text(event.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
                    DetailLine("归档状态", if (event.archivedAt == null) "未归档" else "已归档")
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
                        Text(event.description.ifEmpty { "暂未填写描述。" }, color = if (event.description.isEmpty()) Muted else Ink)
                    }
                }
            }
            state.operationError?.let { ErrorNotice(it) }
            Button(onClick = onComplete, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(if (event.completedAt == null) "标记完成" else "恢复为未完成")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onEdit, enabled = !busy, modifier = Modifier.weight(1f)) { Text("编辑事项") }
                OutlinedButton(onClick = onArchive, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text(if (event.archivedAt == null) "归档" else "恢复归档")
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
                    Text("这件事项及其信息将从本机删除，此操作无法恢复。")
                }
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }, enabled = !busy) {
                    Text("确认删除", color = MaterialTheme.colorScheme.error)
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
        Text("正在读取事项…", color = Muted)
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

private fun urgencyColors(urgency: Urgency): Pair<Color, Color> = when (urgency) {
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
