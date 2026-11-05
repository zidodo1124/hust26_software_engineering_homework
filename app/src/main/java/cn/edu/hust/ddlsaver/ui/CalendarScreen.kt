package cn.edu.hust.ddlsaver.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.Urgency
import cn.edu.hust.ddlsaver.domain.UrgencyRules
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val clockFormat = DateTimeFormatter.ofPattern("HH:mm").withZone(BeijingZone)

@Composable
internal fun CalendarScreen(state: SaverUiState, view: String, onOpen: (String) -> Unit, onCreate: (Long) -> Unit) {
    val today = state.now.atZone(BeijingZone).toLocalDate()
    var anchorText by rememberSaveable { mutableStateOf(today.toString()) }
    var mode by rememberSaveable { mutableStateOf("all") }
    var todayRequest by rememberSaveable { mutableIntStateOf(0) }
    val anchor = LocalDate.parse(anchorText)
    val monthCellHeight = if (LocalDensity.current.fontScale > 1.2f) 100.dp else 72.dp
    val visibleEvents = state.events.filter {
        when (mode) {
            "todo" -> UrgencyRules.isActive(it)
            "done" -> it.completedAt != null && it.archivedAt == null
            else -> true
        }
    }.sortedWith(compareBy<DdlEvent> { it.deadlineAt }.thenBy { it.id })
    val weekStart = anchor.minusDays((anchor.dayOfWeek.value - 1).toLong())
    Column(Modifier.fillMaxSize().testTag("calendar-screen").padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(when(view) { "month" -> "月历"; "week" -> "一周安排"; else -> "当日安排" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("把时间留给重要的事 · 北京时间", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = { anchorText = today.toString(); todayRequest++ }, contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.testTag("calendar-page-today")) { Text("回到今天") }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "全部", "todo" to "待办", "done" to "已完成").forEach { (value, label) ->
                FilterChip(mode == value, { mode = value }, label = { Text(label) }, modifier = Modifier.testTag("calendar-filter-$value"))
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Urgency.NORMAL to "充裕", Urgency.SOON to "临近", Urgency.DANGER to "危险", Urgency.OVERDUE to "逾期",
                Urgency.COMPLETED to "完成", Urgency.ARCHIVED to "归档").forEach { (status, label) ->
                val colors = calendarStatusColors(status)
                Text(label, Modifier.background(colors.second, RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 3.dp),
                    color = colors.first, style = MaterialTheme.typography.labelSmall)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton({ anchorText = when (view) {
                "month" -> anchor.minusMonths(1)
                "week" -> anchor.minusWeeks(1)
                else -> anchor.minusDays(1)
            }.toString() }, modifier = Modifier.testTag("page-previous")) { Text("‹") }
            Text(when (view) {
                "month" -> "${anchor.year}年${anchor.monthValue}月"
                "week" -> "$weekStart 至 ${weekStart.plusDays(6)}"
                else -> anchor.toString()
            }, Modifier.weight(1f).testTag("page-period"), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
            TextButton({ anchorText = when (view) {
                "month" -> anchor.plusMonths(1)
                "week" -> anchor.plusWeeks(1)
                else -> anchor.plusDays(1)
            }.toString() }, modifier = Modifier.testTag("page-next")) { Text("›") }
        }
        if (view == "month") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("已选 ${anchor.monthValue}月${anchor.dayOfMonth}日", Modifier.weight(1f), color = Ocean, style = MaterialTheme.typography.labelLarge)
                FilledTonalButton({ onCreate(anchor.atTime(23, 59).atZone(BeijingZone).toInstant().toEpochMilli()) },
                    modifier = Modifier.testTag("calendar-create-selected")) { Text("＋ 当天新建") }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth()) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                        Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = Muted)
                    }
                }
                calendarWeeks(YearMonth.from(anchor)).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            if (date == null) Spacer(Modifier.weight(1f).height(monthCellHeight))
                            else MonthDay(date, anchor, today, calendarEventsOn(date, visibleEvents), state,
                                Modifier.weight(1f), monthCellHeight, onSelect = { anchorText = date.toString() }, onOpen = onOpen)
                        }
                    }
                }
                val selectedEvents = calendarEventsOn(anchor, visibleEvents)
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${anchor.monthValue}月${anchor.dayOfMonth}日 · ${selectedEvents.size} 件事项",
                        Modifier.weight(1f).testTag("calendar-selected-summary"), fontWeight = FontWeight.SemiBold)
                }
                if (selectedEvents.isEmpty()) Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large) {
                    Text("这一天还没有安排，点击“当天新建”添加。", Modifier.fillMaxWidth().padding(20.dp), color = Muted)
                }
                selectedEvents.forEach { event ->
                    EventCard(event, state, onOpen)
                    Spacer(Modifier.height(8.dp))
                }
            }
        } else {
            CalendarTimeline(if (view == "week") (0L..6L).map(weekStart::plusDays) else listOf(anchor), visibleEvents, state, todayRequest, onOpen, onCreate)
        }
    }
}

@Composable
private fun MonthDay(date: LocalDate, selected: LocalDate, today: LocalDate, events: List<DdlEvent>, state: SaverUiState,
                     modifier: Modifier, cellHeight: androidx.compose.ui.unit.Dp, onSelect: () -> Unit, onOpen: (String) -> Unit) {
    Surface(modifier = modifier.height(cellHeight).padding(1.dp).testTag("page-day-$date"), onClick = onSelect,
        shape = RoundedCornerShape(8.dp), color = if (date == selected) OceanMist else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (date == selected) 1.dp else 0.5.dp, if (date == selected) Ocean else MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(3.dp)) {
            Text(date.dayOfMonth.toString(), modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect).testTag("calendar-date-$date"), color = if (date == today) Ocean else Ink,
                fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
            events.take(2).forEach { event ->
                val colors = calendarColors(event, state)
                Text(event.title, Modifier.fillMaxWidth().padding(top = 2.dp).background(colors.second, RoundedCornerShape(3.dp))
                    .clickable { onOpen(event.id) }.padding(horizontal = 2.dp).testTag("month-title-${event.id}"),
                    color = colors.first, fontSize = 9.sp, lineHeight = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (events.size > 2) Text("+${events.size - 2} 项", fontSize = 10.sp, color = Ocean)
        }
    }
}

@Composable
private fun CalendarTimeline(days: List<LocalDate>, events: List<DdlEvent>,
                             state: SaverUiState, todayRequest: Int, onOpen: (String) -> Unit, onCreate: (Long) -> Unit) {
    val horizontal = rememberScrollState()
    val density = LocalDensity.current
    val localNow = state.now.atZone(BeijingZone)
    val today = localNow.toLocalDate()
    val currentMinute = localNow.hour * 60 + localNow.minute
    val defaultMinute = if (today in days) maxOf(0, currentMinute - 120) else 480
    val vertical = rememberScrollState(initial = with(density) { defaultMinute.dp.roundToPx() })
    var positionedPeriod by rememberSaveable { mutableStateOf<String?>(null) }
    val columns = days.map { date ->
        val placements = timelinePlacements(date, events)
        val maxLanes = placements.maxOfOrNull { it.laneCount } ?: 1
        Triple(date, placements, maxOf(if (days.size == 1) 260.dp else 110.dp, 72.dp * maxLanes))
    }
    LaunchedEffect(days, todayRequest) {
        val period = "${days.first()}:${days.last()}:$todayRequest"
        if (positionedPeriod == period) return@LaunchedEffect
        val minute = if (today in days) maxOf(0, currentMinute - 120) else 480
        vertical.scrollTo(with(density) { minute.dp.roundToPx() })
        val index = days.indexOf(today)
        if (index >= 0) horizontal.scrollTo(with(density) { columns.take(index).sumOf { it.third.value.toDouble() }.toFloat().dp.roundToPx() })
        else horizontal.scrollTo(0)
        positionedPeriod = period
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        var rulesOpen by rememberSaveable { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("点击空白时段新建 · 点击事项查看", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton({ rulesOpen = true }, contentPadding = PaddingValues(horizontal = 8.dp), modifier = Modifier.testTag("calendar-time-rules")) { Text("时间规则") }
        }
        if (rulesOpen) AlertDialog(onDismissRequest = { rulesOpen = false }, title = { Text("日历时间规则") },
            text = { Text("开始时间＝截止时间－速通时长。\n\n未填写速通时长：默认按 60 分钟展示，不启用速通危险期提醒。\n\n点击空白处：按 15 分钟对齐所选开始时刻，截止时间预填为一小时后，可在编辑页修改。\n\n跨日事项会分段展示；短时段会加高以方便点击，具体时间以文字为准。所有时间均为北京时间。") },
            confirmButton = { TextButton({ rulesOpen = false }) { Text("知道了") } })
        Text("开始＝截止－速通时长 · 未填写默认展示 60 分钟", color = Muted, style = MaterialTheme.typography.labelSmall)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
        val availableDayWidth = maxWidth - 44.dp
        val displayColumns = columns.map { (date, placements, width) -> Triple(date, placements, if (days.size == 1) maxOf(width, availableDayWidth) else width) }
        Column {
        Row(Modifier.fillMaxWidth().background(OceanMist, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))) {
            Text("时间", Modifier.width(44.dp).padding(vertical = 12.dp), color = Muted, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            Row(Modifier.weight(1f).horizontalScroll(horizontal)) {
                displayColumns.forEach { (date, _, width) ->
                    Column(Modifier.width(width).padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")[date.dayOfWeek.value - 1], color = Muted, style = MaterialTheme.typography.labelSmall)
                        Text("${date.monthValue}/${date.dayOfMonth}" + if (date == today) " 今天" else "", color = if (date == today) Ocean else Ink, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        Row(Modifier.weight(1f).verticalScroll(vertical).testTag("calendar-timeline")) {
            Column(Modifier.width(44.dp)) {
                (0..23).forEach { hour -> Text("%02d:00".format(hour), Modifier.height(60.dp), fontSize = 11.sp, color = Muted) }
            }
            Row(Modifier.weight(1f).horizontalScroll(horizontal)) {
            displayColumns.forEach { (date, placements, columnWidth) ->
                BoxWithConstraints(Modifier.width(columnWidth).height(1464.dp)
                    .background(if (date == today) Color(0xFFFAFCFF) else MaterialTheme.colorScheme.surface)
                    .testTag("timeline-column-$date")
                    .pointerInput(date, onCreate) { detectTapGestures { point ->
                        val minute = (point.y / density.density).toInt().coerceIn(0, 1439) / 15 * 15
                        onCreate(calendarSlotDeadline(date, minute))
                    } }) {
                    Column { repeat(24) { HorizontalDivider(); Spacer(Modifier.height(59.dp)) } }
                    placements.forEach { placement ->
                        val event = placement.event
                        val colors = calendarColors(event, state)
                        val width = maxWidth / placement.laneCount
                        val start = calendarStart(event).atZone(BeijingZone)
                        val end = Instant.ofEpochMilli(event.deadlineAt).atZone(BeijingZone)
                        val labelOffset = (vertical.value / density.density - placement.startMinute)
                            .coerceIn(0f, maxOf(0f, placement.endMinute - placement.startMinute - 24f))
                        val timeLabel = if (start.toLocalDate() == end.toLocalDate()) {
                            "${clockFormat.format(start)}–${clockFormat.format(end)}"
                        } else "${start.monthValue}/${start.dayOfMonth} ${clockFormat.format(start)}–${end.monthValue}/${end.dayOfMonth} ${clockFormat.format(end)}"
                        Surface(onClick = { onOpen(event.id) }, modifier = Modifier
                            .offset(x = width * placement.lane, y = placement.startMinute.dp)
                            .width(width).height((placement.endMinute - placement.startMinute).dp)
                            .padding(1.dp).testTag("timeline-event-${date}-${event.id}"),
                            color = colors.second, shape = RoundedCornerShape(6.dp), border = BorderStroke(0.5.dp, colors.first.copy(alpha = 0.2f))) {
                            Column(Modifier.offset(y = labelOffset.dp).padding(horizontal = 5.dp, vertical = 2.dp)) {
                                Text(event.title, color = colors.first, fontSize = 11.sp,
                                    lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = if (placement.endMinute - placement.startMinute >= 50) 2 else 1, overflow = TextOverflow.Ellipsis)
                                if (placement.endMinute - placement.startMinute >= 40) Text(timeLabel, color = colors.first, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (placement.endMinute - placement.startMinute >= 60) Text(calendarDurationLabel(event), color = colors.first, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (date == today) Row(Modifier.fillMaxWidth().offset(y = currentMinute.dp).testTag("calendar-current-time"), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.error, RoundedCornerShape(3.dp)))
                        HorizontalDivider(Modifier.weight(1f), thickness = 1.5.dp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            }
        }
        }
        }
    }
}

private fun calendarColors(event: DdlEvent, state: SaverUiState): Pair<Color, Color> =
    calendarStatusColors(UrgencyRules.classify(event, state.now, state.settings.urgentMinutes))

private fun calendarStatusColors(urgency: Urgency): Pair<Color, Color> =
    when (urgency) {
        Urgency.ARCHIVED -> Color(0xFF5C4280) to Color(0xFFEEE5F6)
        else -> urgencyColors(urgency)
    }
