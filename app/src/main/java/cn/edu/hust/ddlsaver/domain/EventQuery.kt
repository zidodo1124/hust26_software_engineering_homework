package cn.edu.hust.ddlsaver.domain

import java.time.Instant
import java.time.LocalDate

enum class EventListMode { TODO, COMPLETED, ARCHIVED }

data class EventFilter(
    val mode: EventListMode = EventListMode.TODO,
    val keyword: String = "",
    val sourceId: String? = null,
    val tagIds: Set<String> = emptySet(),
    val date: LocalDate? = null,
    val dangerOnly: Boolean = false,
)

object EventQuery {
    fun select(
        events: List<DdlEvent>,
        filter: EventFilter,
        now: Instant,
        settings: AppSettings = AppSettings(),
    ): List<DdlEvent> {
        val keyword = filter.keyword.trim()
        return events.filter { event ->
            val inMode = when (filter.mode) {
                EventListMode.TODO -> UrgencyRules.isActive(event)
                EventListMode.COMPLETED -> event.completedAt != null && event.archivedAt == null
                EventListMode.ARCHIVED -> event.archivedAt != null
            }
            inMode &&
                (filter.sourceId == null || event.source?.id == filter.sourceId) &&
                event.tags.map { it.id }.toSet().containsAll(filter.tagIds) &&
                (filter.date == null || Instant.ofEpochMilli(event.deadlineAt)
                    .atZone(UrgencyRules.BEIJING_ZONE).toLocalDate() == filter.date) &&
                (!filter.dangerOnly || UrgencyRules.classify(event, now, settings.urgentMinutes) == Urgency.DANGER) &&
                (keyword.isEmpty() || event.title.contains(keyword, ignoreCase = true) ||
                    event.description.contains(keyword, ignoreCase = true) ||
                    event.customFields.any {
                        it.label.contains(keyword, ignoreCase = true) || it.value.contains(keyword, ignoreCase = true)
                    })
        }.sortedWith(compareBy<DdlEvent> { it.deadlineAt }.thenBy { it.id })
    }
}
