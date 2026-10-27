package cn.edu.hust.ddlsaver.domain

object EventValidation {
    const val MILLIS_PER_MINUTE = 60_000L
    const val MAX_DURATION_MINUTES = Long.MAX_VALUE / MILLIS_PER_MINUTE

    fun normalize(draft: EventDraft): EventDraft {
        val title = draft.title.trim()
        if (title.isBlank()) throw EventValidationException("请填写事项标题。")
        if (draft.id != null && draft.id.isBlank()) {
            throw EventValidationException("事项标识无效，请重新打开。")
        }
        if (draft.id == null && draft.expectedRevision != null) {
            throw EventValidationException("新建事项不能携带旧版本信息。")
        }
        if (draft.id != null && (draft.expectedRevision == null || draft.expectedRevision < 1)) {
            throw EventValidationException("缺少事项版本信息，请重新打开后再编辑。")
        }
        draft.sprintMinutes?.let { validateMinutes(it, "速通时间") }
        val deadline = try {
            Math.multiplyExact(Math.floorDiv(draft.deadlineAt, MILLIS_PER_MINUTE), MILLIS_PER_MINUTE)
        } catch (_: ArithmeticException) {
            throw EventValidationException("截止时间超出可保存范围。")
        }
        return draft.copy(title = title, deadlineAt = deadline)
    }

    fun validateMinutes(minutes: Long, fieldName: String = "普通紧急阈值") {
        if (minutes <= 0) throw EventValidationException("${fieldName}必须大于 0 分钟。")
        if (minutes > MAX_DURATION_MINUTES) throw EventValidationException("${fieldName}过大，请缩短时长。")
    }
}
