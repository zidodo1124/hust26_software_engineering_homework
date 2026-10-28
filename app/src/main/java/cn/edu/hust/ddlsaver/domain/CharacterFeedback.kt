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
    "这里暂时没有待办，先歇一会儿吧。",
    "这会儿没有待办，给自己留点空闲。",
    "新的安排来了，随时记在这里。",
    "现在可以从容安排接下来的事了。",
    "先休息一下，有新任务再来找我。",
    "清单暂时空着，去做点喜欢的事吧。",
)
private val normalLines = listOf(
    "先挑一小步开始，我陪你慢慢推进。",
    "把提交要求看清楚，后面会省不少事。",
    "先做最重要的一件，不用一下子全做完。",
    "提前留好检查和上传的时间。",
    "今天推进一点，最后一天就轻松一点。",
    "任务有点大？先拆成几步再开始。",
)
private val soonLines = listOf(
    "快到截止时间了，看看还差哪一步。",
    "准备收尾吧，附件和提交入口也检查一下。",
    "先完成必交内容，有余力再打磨。",
    "最后检查一下格式，别漏了需要的附件。",
    "先把下一步做好，我们再看剩下的。",
    "快到截止时间了，给上传和确认留点时间。",
)
private val dangerLines = listOf(
    "到速通时间了，先把这件事放在前面。",
    "时间紧了，先做出一份能提交的版本。",
    "先顾核心要求，细节按剩余时间安排。",
    "把干扰放一边，先完成眼前这一步。",
    "冲刺也别漏了上传和提交确认。",
    "我们一步一步来，先完成最关键的部分。",
)
private val overdueLines = listOf(
    "有事项逾期了，先看看能不能补交。",
    "先看一下逾期事项，再决定怎么处理。",
    "不确定还能不能交的话，联系相关人员问问吧。",
    "先别着急，看看现在还能补上哪一步。",
    "这件事仍在清单里，处理后可以完成或归档。",
    "把能补救的部分处理好，再安排下一件。",
)
private val completedLines = listOf(
    "又完成一项，辛苦啦！",
    "清单短了一点，歇口气再继续。",
    "这一项收尾了，给自己一点休息时间。",
    "做得不错！需要提交的内容，记得核对结果。",
    "进度又往前走了一步，辛苦了。",
    "这一项记为完成啦，接下来按自己的节奏来。",
)
