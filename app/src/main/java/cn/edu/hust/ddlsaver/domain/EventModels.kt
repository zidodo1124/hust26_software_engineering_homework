package cn.edu.hust.ddlsaver.domain

enum class Importance { LOW, NORMAL, HIGH }

enum class Urgency { NORMAL, SOON, DANGER, OVERDUE, COMPLETED, ARCHIVED }

enum class ExpiredPolicy { KEEP, AUTO_ARCHIVE }

enum class BatchOperation { COMPLETE, ARCHIVE, DELETE }

data class Category(val id: String, val name: String, val isActive: Boolean = true)

data class CustomField(val id: String, val label: String, val value: String)

data class AttachmentRef(
    val id: String,
    val relativePath: String,
    val mimeType: String,
    val displayName: String,
    val byteSize: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
)

data class DdlEvent(
    val id: String,
    val title: String,
    val deadlineAt: Long,
    val sprintMinutes: Long?,
    val description: String,
    val importance: Importance,
    val completedAt: Long?,
    val archivedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long,
    val source: Category? = null,
    val tags: List<Category> = emptyList(),
    val customFields: List<CustomField> = emptyList(),
    val attachments: List<AttachmentRef> = emptyList(),
)

data class EventDraft(
    val id: String? = null,
    val title: String,
    val deadlineAt: Long,
    val sprintMinutes: Long? = null,
    val description: String = "",
    val importance: Importance = Importance.NORMAL,
    val expectedRevision: Long? = null,
    val sourceId: String? = null,
    val tagIds: Set<String> = emptySet(),
    val customFields: List<CustomField> = emptyList(),
    val attachments: List<AttachmentRef> = emptyList(),
)

data class AppSettings(
    val urgentMinutes: Long = 24 * 60L,
    val expiredPolicy: ExpiredPolicy = ExpiredPolicy.KEEP,
    val dailySummaryEnabled: Boolean = true,
    val dailySummaryMinuteOfDay: Int = 8 * 60,
    val dangerReminderEnabled: Boolean = true,
    val deadlineReminderEnabled: Boolean = true,
    val deadlineLeadMinutes: Long = 60,
    val characterStyle: CharacterStyle = CharacterStyle.CLASSIC,
)

class EventValidationException(message: String) : IllegalArgumentException(message)

class EventNotFoundException : IllegalStateException("这条事项已不存在，请返回列表刷新。")

class EventConflictException : IllegalStateException("这条事项已被修改，请重新打开后再编辑。")
