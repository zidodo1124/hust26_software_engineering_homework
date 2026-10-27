package cn.edu.hust.ddlsaver.domain

enum class Importance { LOW, NORMAL, HIGH }

enum class Urgency { NORMAL, SOON, DANGER, OVERDUE, COMPLETED, ARCHIVED }

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
)

data class EventDraft(
    val id: String? = null,
    val title: String,
    val deadlineAt: Long,
    val sprintMinutes: Long? = null,
    val description: String = "",
    val importance: Importance = Importance.NORMAL,
    val expectedRevision: Long? = null,
)

data class AppSettings(val urgentMinutes: Long = 24 * 60L)

class EventValidationException(message: String) : IllegalArgumentException(message)

class EventNotFoundException : IllegalStateException("这条事项已不存在，请返回列表刷新。")

class EventConflictException : IllegalStateException("这条事项已被修改，请重新打开后再编辑。")
