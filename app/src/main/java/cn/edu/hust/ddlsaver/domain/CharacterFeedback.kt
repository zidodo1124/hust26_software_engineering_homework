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
        CharacterState.NORMAL -> Triple(if (active.isEmpty()) "暂时没待办" else "先做一件", "maid-left.webp", "鲸鱼娘普通表情")
        CharacterState.SOON -> Triple("快到点了", "whale-serious.webp", "鲸鱼娘严肃表情")
        CharacterState.DANGER -> Triple("该冲刺了", "whale-angry.webp", "鲸鱼娘着急催促的表情")
        CharacterState.OVERDUE -> Triple("有事逾期了", "whale-serious.webp", "鲸鱼娘认真提醒的表情")
        CharacterState.COMPLETED -> Triple("又搞定一件", "whale-cheerful.webp", "鲸鱼娘快乐表情")
    }
    return CharacterFeedback(state, title, line, "character/$asset", description,
        focusEventId = if (justCompleted) null else focus?.first?.id)
}

private val emptyLines = listOf(
    "这会儿没待办，歇会儿吧。",
    "清单空着呢，空闲时间留给自己。",
    "有新安排了，记在这儿就行。",
    "趁现在有空，想想接下来要做什么。",
    "先去忙别的，有新任务再记。",
    "暂时没事要赶，做点喜欢的事吧。",
)
private val normalLines = listOf(
    "先挑一件，做一点算一点。",
    "开工前看清要求，省得返工。",
    "先忙最要紧的，其他的慢慢来。",
    "记得给检查和上传留点时间。",
    "今天多做一点，最后一天少赶一点。",
    "不好下手的话，先拆成几件小事。",
)
private val soonLines = listOf(
    "快到点了，看看还差什么。",
    "该收尾了，附件和提交入口也看一眼。",
    "先把要交的做完，细节待会儿再改。",
    "交之前看一眼格式，附件别漏了。",
    "别急，先把手上这一步做完。",
    "快截止了，别把上传拖到最后一刻。",
)
private val dangerLines = listOf(
    "到冲刺时间了，先忙这件吧。",
    "时间不多了，先做出能交的版本。",
    "先把最要紧的做完，细节有空再改。",
    "先别分心，忙完手上这一步。",
    "忙归忙，别忘了上传后确认一下。",
    "别慌，先把最关键的部分做完。",
)
private val overdueLines = listOf(
    "这件事过点了，看看还能不能补交。",
    "有件事逾期了，点开看看怎么处理。",
    "还能不能补交？找负责人问问吧。",
    "先别急，看看还有什么能补救。",
    "这件事还留着，处理好再完成或归档。",
    "能补的先补上，再忙下一件。",
)
private val completedLines = listOf(
    "又搞定一件，辛苦啦！",
    "少了一件待办，歇口气吧。",
    "这件事忙完了，休息一会儿。",
    "做完啦！要交的东西，记得看看交上没有。",
    "又做完一件，今天没白忙。",
    "这一项记为完成了，接着忙还是歇会儿？",
)
