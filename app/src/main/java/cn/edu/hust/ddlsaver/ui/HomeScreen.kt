package cn.edu.hust.ddlsaver.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.hust.ddlsaver.domain.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun HomeScreen(
    state: SaverUiState, batchToken: Long, onSettings: () -> Unit, onOpen: (String) -> Unit,
    onBatch: (Set<String>, BatchOperation) -> Unit,
    initiallyToday: Boolean = false, reminderWarning: String? = null,
) {
    var modeName by rememberSaveable { mutableStateOf(EventListMode.TODO.name) }
    var quickFilter by rememberSaveable { mutableStateOf(if (initiallyToday) "today" else "all") }
    var query by rememberSaveable { mutableStateOf("") }
    var sourceId by rememberSaveable { mutableStateOf<String?>(null) }
    var tagsJson by rememberSaveable { mutableStateOf("[]") }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    var calendarOpen by rememberSaveable { mutableStateOf(false) }
    var calendarView by rememberSaveable { mutableStateOf("month") }
    var calendarAnchor by rememberSaveable { mutableStateOf(state.now.atZone(BeijingZone).toLocalDate().toString()) }
    var selectedDate by rememberSaveable { mutableStateOf<String?>(null) }
    var monthText by rememberSaveable { mutableStateOf(YearMonth.from(state.now.atZone(BeijingZone)).toString()) }
    var selecting by rememberSaveable { mutableStateOf(false) }
    var selectionJson by rememberSaveable { mutableStateOf("[]") }
    var deleteJson by rememberSaveable { mutableStateOf<String?>(null) }
    var seenBatchToken by rememberSaveable { mutableLongStateOf(batchToken) }
    LaunchedEffect(batchToken) { if (batchToken != seenBatchToken) {
        seenBatchToken = batchToken; selecting = false; selectionJson = "[]"; deleteJson = null
    } }
    val selected = DraftJson.ids(selectionJson)
    val tagIds = DraftJson.ids(tagsJson)
    val active = state.events.filter(UrgencyRules::isActive)
    val today = state.now.atZone(BeijingZone).toLocalDate()
    val todayCount = active.count { UrgencyRules.isToday(it, state.now) }
    val dangerCount = active.count { UrgencyRules.classify(it, state.now, state.settings.urgentMinutes) == Urgency.DANGER }
    val mode = EventListMode.valueOf(modeName)
    val anchorDate = LocalDate.parse(calendarAnchor)
    val weekStart = anchorDate.minusDays((anchorDate.dayOfWeek.value - 1).toLong())
    val inWeekView = calendarOpen && calendarView == "week" && quickFilter == "all"
    val dateFilter = if (quickFilter == "today") today else if (inWeekView) null else selectedDate?.let(LocalDate::parse)
    val filtered = EventQuery.select(state.events, EventFilter(mode = mode, keyword = query, sourceId = sourceId,
        tagIds = tagIds, date = dateFilter, dangerOnly = quickFilter == "danger"), state.now, state.settings)
        .filter { event ->
            val date = java.time.Instant.ofEpochMilli(event.deadlineAt).atZone(BeijingZone).toLocalDate()
            !inWeekView || (date >= weekStart && date < weekStart.plusWeeks(1))
        }
    // The calendar shows the whole month's distribution, not just the selected day.
    val calendarEvents = EventQuery.select(state.events, EventFilter(mode = mode, keyword = query,
        sourceId = sourceId, tagIds = tagIds, dangerOnly = quickFilter == "danger"), state.now, state.settings)
    val calendarCounts = calendarEvents.groupingBy {
        java.time.Instant.ofEpochMilli(it.deadlineAt).atZone(BeijingZone).toLocalDate()
    }.eachCount()
    val overdue = if (mode == EventListMode.TODO) filtered.filter { it.deadlineAt <= state.now.toEpochMilli() } else emptyList()
    val current = if (mode == EventListMode.TODO) filtered.filter { it.deadlineAt > state.now.toEpochMilli() } else filtered
    val busy = state.busy || state.actionResult != null
    fun clearFilters() { sourceId = null; tagsJson = "[]"; query = ""; selectedDate = null; quickFilter = "all"; calendarView = "month" }
    fun toggleSelection(id: String) { selectionJson = DraftJson.ids(if (id in selected) selected - id else selected + id) }

    LazyColumn(Modifier.fillMaxSize().testTag("event-list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("HUSTer's", color = Muted, style = MaterialTheme.typography.labelLarge)
                    Text("DDL Saver", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onSettings, enabled = !busy) { Text("设置") }
            }
        }
        item {
            Surface(color = OceanMist, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, OceanBorder)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("重要的事，提前安排。", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(DateTimeFormatter.ofPattern("M月d日 EEEE  HH:mm", Locale.CHINA).withZone(BeijingZone).format(state.now) + " · 北京时间",
                        color = Muted, style = MaterialTheme.typography.bodySmall)
                    val fontScale = LocalDensity.current.fontScale
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val todayClick = { clearFilters(); modeName = EventListMode.TODO.name; quickFilter = "today" }
                        val dangerClick = { clearFilters(); modeName = EventListMode.TODO.name; quickFilter = "danger" }
                        // Let labels and counts grow vertically on narrow screens or with larger text.
                        if (maxWidth < 280.dp || fontScale > 1.3f) {
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                CountTile("今日 DDL", todayCount, Modifier.fillMaxWidth().testTag("today-count"), Ocean, todayClick)
                                CountTile("速通危险期", dangerCount, Modifier.fillMaxWidth().testTag("danger-count"), MaterialTheme.colorScheme.error, dangerClick)
                            }
                        } else {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CountTile("今日 DDL", todayCount, Modifier.weight(1f).testTag("today-count"), Ocean, todayClick)
                                CountTile("速通危险期", dangerCount, Modifier.weight(1f).testTag("danger-count"), MaterialTheme.colorScheme.error, dangerClick)
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(EventListMode.TODO to "待办", EventListMode.COMPLETED to "已完成", EventListMode.ARCHIVED to "归档").forEach { (key, label) ->
                    FilterChip(mode == key, { modeName = key.name; quickFilter = "all" }, label = { Text(label) })
                }
            }
            OutlinedTextField(query, { query = it }, singleLine = true, label = { Text("搜索标题、描述或自定义字段") },
                modifier = Modifier.fillMaxWidth().testTag("event-search"), shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton({ filtersOpen = !filtersOpen }, modifier = Modifier.testTag("toggle-filters")) { Text("来源 / 标签") }
                TextButton({ calendarOpen = !calendarOpen }, modifier = Modifier.testTag("toggle-calendar")) { Text("日历") }
                TextButton({ selecting = !selecting; selectionJson = "[]" }, enabled = !busy, modifier = Modifier.testTag("toggle-batch")) { Text(if (selecting) "退出批量" else "批量管理") }
            }
        }
        item { CharacterPanel(selectCharacterFeedback(state.events, state.now, state.settings, state.justCompleted), style = state.settings.characterStyle) }
        reminderWarning?.let { warning -> item {
            Surface(color = Color(0xFFFFF2D9), shape = RoundedCornerShape(14.dp)) {
                Text(warning, Modifier.fillMaxWidth().padding(14.dp).testTag("home-reminder-warning"), color = Ink,
                    style = MaterialTheme.typography.bodySmall)
            }
        } }
        if (filtersOpen) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("来源（单选）", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(sourceId == null, { sourceId = null }, label = { Text("全部来源") })
                    state.sources.forEach { item -> FilterChip(sourceId == item.id, { sourceId = item.id },
                        label = { Text(item.name + if (item.isActive) "" else "（已停用）") }, modifier = Modifier.testTag("filter-source-${item.id}")) }
                }
                Text("标签（需同时包含所选标签）", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.tags.forEach { item -> FilterChip(item.id in tagIds,
                        { tagsJson = DraftJson.ids(if (item.id in tagIds) tagIds - item.id else tagIds + item.id) },
                        label = { Text(item.name + if (item.isActive) "" else "（已停用）") }, modifier = Modifier.testTag("filter-tag-${item.id}")) }
                }
            }
        }
        if (calendarOpen) item {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("month" to "月", "week" to "周", "day" to "日").forEach { (view, label) ->
                        FilterChip(calendarView == view, {
                            calendarView = view
                            if (view != "month") {
                                val date = selectedDate?.let(LocalDate::parse) ?: anchorDate
                                calendarAnchor = date.toString()
                                selectedDate = date.toString()
                                quickFilter = "all"
                            }
                        }, label = { Text(label) }, modifier = Modifier.testTag("calendar-view-$view"))
                    }
                }
                if (calendarView == "month") {
                    MonthCalendar(YearMonth.parse(monthText), selectedDate?.let(LocalDate::parse), today, calendarCounts,
                        onMonth = { monthText = it.toString() }, onDate = {
                            selectedDate = it.toString(); calendarAnchor = it.toString(); quickFilter = "all"
                        })
                } else {
                    CalendarPeriod(calendarView, anchorDate, today, calendarCounts, onNavigate = { date ->
                        calendarAnchor = date.toString(); selectedDate = date.toString()
                        monthText = YearMonth.from(date).toString(); quickFilter = "all"
                    }, onDay = { date ->
                        calendarAnchor = date.toString(); selectedDate = date.toString()
                        monthText = YearMonth.from(date).toString(); quickFilter = "all"; calendarView = "day"
                    })
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text((when {
                    quickFilter == "today" -> "今日待办"
                    quickFilter == "danger" -> "危险期待办"
                    inWeekView -> "$weekStart 至 ${weekStart.plusDays(6)} 的事项"
                    selectedDate != null -> "$selectedDate 的事项"
                    mode == EventListMode.COMPLETED -> "已完成事项"
                    mode == EventListMode.ARCHIVED -> "归档事项"
                    else -> "全部待办"
                }) + " · ${filtered.size}", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).testTag("list-summary"))
                TextButton({ clearFilters() }, modifier = Modifier.testTag("clear-filters")) { Text("清除筛选") }
            }
            if (selecting) {
                Text("已选 ${selected.size} 件。切换筛选不会清空已选事项。", color = Muted)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    TextButton({ selectionJson = DraftJson.ids(selected + filtered.map { it.id }) }, enabled = !busy) { Text("全选当前结果") }
                    TextButton({ selectionJson = "[]" }, enabled = !busy) { Text("清空选择") }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    TextButton({ onBatch(selected.toSet(), BatchOperation.COMPLETE) }, enabled = !busy && selected.isNotEmpty(), modifier = Modifier.testTag("batch-complete")) { Text("批量完成") }
                    TextButton({ onBatch(selected.toSet(), BatchOperation.ARCHIVE) }, enabled = !busy && selected.isNotEmpty(), modifier = Modifier.testTag("batch-archive")) { Text("批量归档") }
                    TextButton({ deleteJson = DraftJson.ids(selected) }, enabled = !busy && selected.isNotEmpty(), modifier = Modifier.testTag("batch-delete")) { Text("批量删除") }
                }
            }
            state.operationError?.let { ErrorNotice(it) }
        }
        if (filtered.isEmpty()) item {
            Text(when {
                query.isNotBlank() || sourceId != null || tagIds.isNotEmpty() || selectedDate != null ->
                    "没有找到符合条件的事项。可以调整条件，或点击“清除筛选”。"
                quickFilter == "today" -> "今天暂无待办。点击“清除筛选”可查看其他日期的事项。"
                quickFilter == "danger" -> "目前没有处于速通危险期的事项。点击“清除筛选”可查看全部待办。"
                mode == EventListMode.COMPLETED -> "暂无已完成事项。完成事项后，可在这里查看记录。"
                mode == EventListMode.ARCHIVED -> "暂无归档事项。归档可保留记录，并停止相关提醒。"
                else -> "暂无待办。点击“新建事项”，记下下一件需要完成的事。"
            }, color = Muted, modifier = Modifier.padding(vertical = 20.dp))
        }
        items(current, key = { it.id }) { event ->
            EventRow(event, state, selecting, event.id in selected, busy, { toggleSelection(event.id) }, onOpen)
        }
        if (overdue.isNotEmpty()) {
            item { Text("已逾期 · ${overdue.size}", color = Muted, style = MaterialTheme.typography.titleSmall) }
            items(overdue, key = { it.id }) { event ->
                EventRow(event, state, selecting, event.id in selected, busy, { toggleSelection(event.id) }, onOpen)
            }
        }
    }
    deleteJson?.let { frozenJson ->
        val frozen = DraftJson.ids(frozenJson)
        AlertDialog(onDismissRequest = { if (!busy) deleteJson = null }, title = { Text("永久删除选中的 ${frozen.size} 件事项？") },
            text = { Text("所选事项及其附件副本将永久删除，无法恢复。相册原图不受影响。") },
            confirmButton = { TextButton({ deleteJson = null; onBatch(frozen, BatchOperation.DELETE) }, enabled = !busy,
                modifier = Modifier.testTag("batch-confirm-delete")) { Text("确认删除") } },
            dismissButton = { TextButton({ deleteJson = null }, enabled = !busy) { Text("取消") } })
    }
}

@Composable
private fun CalendarPeriod(view: String, anchor: LocalDate, today: LocalDate, counts: Map<LocalDate, Int>,
                           onNavigate: (LocalDate) -> Unit, onDay: (LocalDate) -> Unit) {
    val start = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
    val step = if (view == "week") 7L else 1L
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton({ onNavigate(anchor.minusDays(step)) }, modifier = Modifier.testTag("period-previous")) { Text("‹") }
                Text(if (view == "week") "$start 至 ${start.plusDays(6)}" else anchor.toString(),
                    modifier = Modifier.weight(1f).testTag("period-title"), textAlign = TextAlign.Center)
                TextButton({ onNavigate(anchor.plusDays(step)) }, modifier = Modifier.testTag("period-next")) { Text("›") }
            }
            TextButton({ onNavigate(today) }, modifier = Modifier.align(Alignment.End).testTag("period-today")) { Text("今天") }
            if (view == "week") Row(Modifier.fillMaxWidth()) {
                (0L..6L).forEach { offset ->
                    val date = start.plusDays(offset)
                    TextButton({ onDay(date) }, modifier = Modifier.weight(1f).testTag("week-day-$date"),
                        contentPadding = PaddingValues(0.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(listOf("一", "二", "三", "四", "五", "六", "日")[offset.toInt()])
                            Text(date.dayOfMonth.toString())
                            counts[date]?.let { Text("${it}件", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
            Text(if (view == "week") "查看本周事项；点击日期查看当天。逾期待办单独展示。" else "查看当天事项；逾期待办单独展示。",
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EventRow(event: DdlEvent, state: SaverUiState, selecting: Boolean, selected: Boolean, busy: Boolean,
                     onSelect: () -> Unit, onOpen: (String) -> Unit) {
    Column {
        if (selecting) Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(selected, { onSelect() }, enabled = !busy, modifier = Modifier.testTag("select-${event.id}"))
            Text(if (selected) "已选择" else "选择此事项", style = MaterialTheme.typography.labelMedium)
        }
        EventCard(event, state) { if (selecting) { if (!busy) onSelect() } else onOpen(it) }
    }
}

@Composable
internal fun MonthCalendar(month: YearMonth, selected: LocalDate?, today: LocalDate, counts: Map<LocalDate, Int>, onMonth: (YearMonth) -> Unit, onDate: (LocalDate) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton({ onMonth(month.minusMonths(1)) }, modifier = Modifier.testTag("calendar-previous")) { Text("‹") }
                Text("${month.year}年${month.monthValue}月", modifier = Modifier.weight(1f).testTag("calendar-month"), fontWeight = FontWeight.SemiBold)
                TextButton({ onMonth(month.plusMonths(1)) }, modifier = Modifier.testTag("calendar-next")) { Text("›") }
            }
            TextButton({ onMonth(YearMonth.from(today)); onDate(today) }, modifier = Modifier.align(Alignment.End)) { Text("今天") }
            Row { listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Muted)
            } }
            calendarWeeks(month).forEach { week -> Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    if (date == null) Spacer(Modifier.weight(1f).heightIn(min = 48.dp))
                    else TextButton({ onDate(date) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("calendar-day-$date"),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.textButtonColors(containerColor = if (date == selected) Ocean else Color.Transparent,
                            contentColor = if (date == selected) Color.White else if (date == today) Ocean else Ink)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(date.dayOfMonth.toString())
                            counts[date]?.let { count ->
                                Text("${count}件", style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1, modifier = Modifier.testTag("calendar-count-$date"))
                            }
                        }
                    }
                }
            } }
            Text("日期下方显示符合当前列表和筛选条件的事项数；选择日期查看详情。“今日 DDL”始终统计北京时间的今天。", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

internal fun calendarWeeks(month: YearMonth): List<List<LocalDate?>> {
    val offset = month.atDay(1).dayOfWeek.value - 1
    val cells = MutableList<LocalDate?>(offset) { null }
    (1..month.lengthOfMonth()).forEach { cells.add(month.atDay(it)) }
    while (cells.size % 7 != 0) cells.add(null)
    return cells.chunked(7)
}
