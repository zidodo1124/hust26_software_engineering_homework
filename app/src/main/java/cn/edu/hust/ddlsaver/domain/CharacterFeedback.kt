package cn.edu.hust.ddlsaver.domain

import java.time.Instant

enum class CharacterState { NORMAL, SOON, DANGER, OVERDUE, COMPLETED }

data class CharacterFeedback(
    val state: CharacterState,
    val title: String,
    val line: String,
    val imageAsset: String,
    val imageDescription: String,
    val focusEventId: String? = null,
)

/** A successful completion may briefly override the normal status; old completed rows never do. */
fun selectCharacterFeedback(
    events: List<DdlEvent>,
    now: Instant,
    settings: AppSettings,
    justCompleted: Boolean = false,
): CharacterFeedback {
    val active = events.filter(UrgencyRules::isActive)
    val classified = active.map { it to UrgencyRules.classify(it, now, settings.urgentMinutes) }
    // Imminent, still-actionable work takes priority over already missed deadlines.
    val focus = listOf(Urgency.DANGER, Urgency.OVERDUE, Urgency.SOON, Urgency.NORMAL)
        .firstNotNullOfOrNull { urgency ->
            classified.filter { it.second == urgency }
                .minWithOrNull(compareBy<Pair<DdlEvent, Urgency>> { it.first.deadlineAt }.thenBy { it.first.id })
        }
    val state = if (justCompleted) CharacterState.COMPLETED else when (focus?.second) {
        Urgency.DANGER -> CharacterState.DANGER
        Urgency.OVERDUE -> CharacterState.OVERDUE
        Urgency.SOON -> CharacterState.SOON
        else -> CharacterState.NORMAL
    }
    val lines = when (state) {
        CharacterState.NORMAL -> if (active.isEmpty()) emptyLines else normalLines
        CharacterState.SOON -> soonLines
        CharacterState.DANGER -> dangerLines
        CharacterState.OVERDUE -> overdueLines
        CharacterState.COMPLETED -> completedLines
    }
    // Stable for ten minutes and independent from list order, with no random or network dependency.
    val bucket = Math.floorDiv(now.epochSecond, 600L)
    val seed = (if (justCompleted) "completed" else focus?.first?.id ?: "empty").hashCode().toLong()
    val line = lines[Math.floorMod(bucket + seed, lines.size.toLong()).toInt()]
    val (title, asset, description) = when (state) {
        CharacterState.NORMAL -> Triple(if (active.isEmpty()) "暂时没有待办" else "按计划推进", "maid-left.webp", "鲸鱼娘普通表情")
        CharacterState.SOON -> Triple("临近截止", "whale-serious.webp", "鲸鱼娘严肃表情")
        CharacterState.DANGER -> Triple("进入速通危险期", "whale-angry.webp", "鲸鱼娘着急催促的表情")
        CharacterState.OVERDUE -> Triple("有事项已经逾期", "whale-serious.webp", "鲸鱼娘认真提醒的表情")
        CharacterState.COMPLETED -> Triple("又完成一项", "whale-cheerful.webp", "鲸鱼娘快乐表情")
    }
    return CharacterFeedback(state, title, line, "character/$asset", description,
        focusEventId = if (justCompleted) null else focus?.first?.id)
}

private val emptyLines = listOf(
    "待办清空了，给自己留一点休息时间吧。",
    "现在没有未完成的事项，可以从容安排下一步。",
    "海面平静，新的安排出现时再记下来。",
    "今天的节奏由你掌握，别忘了照顾自己。",
    "先休息一下，有新的截止时间再来告诉我。",
    "没有待办也很好，留一点时间给喜欢的事情。",
)
private val normalLines = listOf(
    "时间还充裕，先完成一个小步骤吧。",
    "把大任务拆开，今天推进一点就有收获。",
    "先确认提交要求，再按自己的节奏开始。",
    "不用等到最后一刻，现在准备会轻松一些。",
    "给检查和提交留些余量，计划会更稳妥。",
    "挑一件重要的事专注一会儿，我在这里陪你。",
)
private val soonLines = listOf(
    "截止时间正在靠近，先看看哪些内容还没完成。",
    "可以开始收尾了，别忘了检查附件和提交入口。",
    "先把必需部分完成，再考虑额外的完善。",
    "留一点时间检查格式，提交时会更从容。",
    "下一步是什么？选一个最小的动作先做起来。",
    "临近截止啦，提前确认网络和提交方式会更稳妥。",
)
private val dangerLines = listOf(
    "已经进入你设置的速通时间，先集中处理这件事。",
    "现在优先完成能提交的版本，记得留出上传时间。",
    "先守住核心要求，一步一步完成最后的冲刺。",
    "时间比较紧，先核对必交内容，再安排剩余步骤。",
    "暂时收起干扰，把注意力留给眼前这一步。",
    "冲刺也要稳住，完成后再确认是否提交成功。",
)
private val overdueLines = listOf(
    "有事项过了截止时间，先确认还能否补交或调整。",
    "先查看逾期事项的实际情况，再决定下一步。",
    "截止时间已过，可以联系相关人员确认处理方式。",
    "别急着否定自己，先把可以补救的步骤列出来。",
    "过期事项仍然保留，需要时可以完成或归档。",
    "先处理能挽回的部分，再为后续安排留出余量。",
)
private val completedLines = listOf(
    "完成了一项，给这份努力一个肯定吧。",
    "又推进一步，辛苦了，稍微放松一下。",
    "这件事已经完成，可以安心看看下一项了。",
    "做到了！记得确认实际提交结果，再继续前进。",
    "清单少了一项，今天的努力有了具体的成果。",
    "完成的感觉不错吧，休息片刻再出发。",
)
