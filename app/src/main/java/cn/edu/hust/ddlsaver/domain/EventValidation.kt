package cn.edu.hust.ddlsaver.domain

object EventValidation {
    const val MILLIS_PER_MINUTE = 60_000L
    const val MAX_DURATION_MINUTES = Long.MAX_VALUE / MILLIS_PER_MINUTE

    fun normalize(draft: EventDraft): EventDraft {
        val title = draft.title.trim()
        if (title.isBlank()) throw EventValidationException("给事项填个标题吧。")
        if (draft.id != null && draft.id.isBlank()) {
            throw EventValidationException("事项信息有误，请重新打开。")
        }
        if (draft.id == null && draft.expectedRevision != null) {
            throw EventValidationException("新建事项不能携带旧版本信息。")
        }
        if (draft.id != null && (draft.expectedRevision == null || draft.expectedRevision < 1)) {
            throw EventValidationException("事项版本信息不完整，请重新打开再编辑。")
        }
        draft.sprintMinutes?.let { validateMinutes(it, "速通时间") }
        if (draft.sourceId?.isBlank() == true || draft.tagIds.any { it.isBlank() }) {
            throw EventValidationException("来源或标签信息有误，请重新选择。")
        }
        val fields = draft.customFields.map { field ->
            if (field.id.isBlank() || field.label.isBlank()) {
                throw EventValidationException("给自定义字段填个名称吧，不用的可以移除。")
            }
            field.copy(label = field.label.trim())
        }
        if (fields.map { it.id }.distinct().size != fields.size) {
            throw EventValidationException("自定义字段信息冲突，请移除后重新添加。")
        }
        val attachments = draft.attachments.map { attachment ->
            if (attachment.id.isBlank() || !safeAttachmentName.matches(attachment.relativePath) ||
                attachment.relativePath != "${attachment.id}.img" ||
                !attachment.mimeType.startsWith("image/") || attachment.byteSize < 0 ||
                attachment.width < 0 || attachment.height < 0
            ) {
                throw EventValidationException("图片信息有误，请重新添加。")
            }
            attachment.copy(displayName = attachment.displayName.trim().ifEmpty { "图片" })
        }
        if (attachments.map { it.id }.distinct().size != attachments.size ||
            attachments.map { it.relativePath }.distinct().size != attachments.size
        ) {
            throw EventValidationException("同一个图片附件重复了，请移除重复项。")
        }
        val deadline = try {
            Math.multiplyExact(Math.floorDiv(draft.deadlineAt, MILLIS_PER_MINUTE), MILLIS_PER_MINUTE)
        } catch (_: ArithmeticException) {
            throw EventValidationException("这个截止时间超出了支持范围，请重新选择。")
        }
        return draft.copy(
            title = title, deadlineAt = deadline, tagIds = draft.tagIds.toSet(),
            customFields = fields, attachments = attachments,
        )
    }

    private val safeAttachmentName = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.img")

    fun normalizeCategoryName(name: String): String = name.trim().also {
        if (it.isBlank()) throw EventValidationException("给分类填个名称吧。")
    }

    fun validateSettings(settings: AppSettings) {
        validateMinutes(settings.urgentMinutes)
        validateMinutes(settings.deadlineLeadMinutes, "临近通知提前量")
        if (settings.dailySummaryMinuteOfDay !in 0..1439) {
            throw EventValidationException("每日汇总时间要在 00:00 至 23:59 之间。")
        }
    }

    fun validateMinutes(minutes: Long, fieldName: String = "普通紧急阈值") {
        if (minutes <= 0) throw EventValidationException("${fieldName}要大于 0 分钟。")
        if (minutes > MAX_DURATION_MINUTES) throw EventValidationException("${fieldName}太长了，请缩短一些。")
    }
}
