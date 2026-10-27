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
        if (draft.sourceId?.isBlank() == true || draft.tagIds.any { it.isBlank() }) {
            throw EventValidationException("来源或标签标识无效，请重新选择。")
        }
        val fields = draft.customFields.map { field ->
            if (field.id.isBlank() || field.label.isBlank()) {
                throw EventValidationException("每个自定义字段都需要名称；不用的字段可以移除。")
            }
            field.copy(label = field.label.trim())
        }
        if (fields.map { it.id }.distinct().size != fields.size) {
            throw EventValidationException("自定义字段标识重复，请移除重复字段。")
        }
        val attachments = draft.attachments.map { attachment ->
            if (attachment.id.isBlank() || !safeAttachmentName.matches(attachment.relativePath) ||
                attachment.relativePath != "${attachment.id}.img" ||
                !attachment.mimeType.startsWith("image/") || attachment.byteSize < 0 ||
                attachment.width < 0 || attachment.height < 0
            ) {
                throw EventValidationException("图片附件信息无效，请重新导入。")
            }
            attachment.copy(displayName = attachment.displayName.trim().ifEmpty { "图片" })
        }
        if (attachments.map { it.id }.distinct().size != attachments.size ||
            attachments.map { it.relativePath }.distinct().size != attachments.size
        ) {
            throw EventValidationException("同一图片附件不能重复加入。")
        }
        val deadline = try {
            Math.multiplyExact(Math.floorDiv(draft.deadlineAt, MILLIS_PER_MINUTE), MILLIS_PER_MINUTE)
        } catch (_: ArithmeticException) {
            throw EventValidationException("截止时间超出可保存范围。")
        }
        return draft.copy(
            title = title, deadlineAt = deadline, tagIds = draft.tagIds.toSet(),
            customFields = fields, attachments = attachments,
        )
    }

    private val safeAttachmentName = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.img")

    fun normalizeCategoryName(name: String): String = name.trim().also {
        if (it.isBlank()) throw EventValidationException("请填写分类名称。")
    }

    fun validateMinutes(minutes: Long, fieldName: String = "普通紧急阈值") {
        if (minutes <= 0) throw EventValidationException("${fieldName}必须大于 0 分钟。")
        if (minutes > MAX_DURATION_MINUTES) throw EventValidationException("${fieldName}过大，请缩短时长。")
    }
}
