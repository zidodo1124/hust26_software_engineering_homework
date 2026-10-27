package cn.edu.hust.ddlsaver.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class EventQueryTest {
    private val now = Instant.parse("2030-01-02T15:00:00Z")
    private val course = Category("course", "课程群")
    private val assignment = Category("assignment", "作业")
    private val team = Category("team", "小组")

    private fun event(id: String, deadline: String = "2030-01-02T15:30:00Z") = DdlEvent(
        id, "课程报告", Instant.parse(deadline).toEpochMilli(), 60, "提交说明", Importance.NORMAL,
        null, null, now.toEpochMilli(), now.toEpochMilli(), 1,
        source = course, tags = listOf(assignment, team),
        customFields = listOf(CustomField("place", "地点", "东九楼 A101")),
    )

    @Test
    fun combinedFiltersRequireEveryConditionAndEverySelectedTag() {
        val matching = event("match")
        val rows = listOf(
            matching,
            event("wrong-source").copy(source = Category("other", "其他")),
            event("one-tag").copy(tags = listOf(assignment)),
            event("wrong-keyword").copy(customFields = emptyList()),
            event("tomorrow", "2030-01-02T16:00:00Z"),
            event("no-sprint").copy(sprintMinutes = null),
            event("done").copy(completedAt = now.toEpochMilli()),
        )
        val filter = EventFilter(
            keyword = "  A101 ", sourceId = course.id, tagIds = setOf(assignment.id, team.id),
            date = LocalDate.of(2030, 1, 2), dangerOnly = true,
        )
        assertEquals(listOf(matching), EventQuery.select(rows, filter, now))
    }

    @Test
    fun keywordMatchesTitleDescriptionAndCustomFieldLabelsOrValuesIgnoringCase() {
        val rows = listOf(event("one"))
        listOf("报告", "提交说明", "地点", "a101").forEach { keyword ->
            assertEquals(rows, EventQuery.select(rows, EventFilter(keyword = keyword), now))
        }
        assertTrue(EventQuery.select(rows, EventFilter(keyword = "不存在"), now).isEmpty())
    }

    @Test
    fun dateUsesBeijingMidnightAndWorksWithCompletedOrArchivedModes() {
        val before = event("before", "2030-01-02T15:59:00Z").copy(completedAt = 1)
        val after = event("after", "2030-01-02T16:00:00Z").copy(completedAt = 1, archivedAt = 2)
        val rows = listOf(before, after)
        assertEquals(listOf(before), EventQuery.select(rows,
            EventFilter(mode = EventListMode.COMPLETED, date = LocalDate.of(2030, 1, 2)), now))
        assertEquals(listOf(after), EventQuery.select(rows,
            EventFilter(mode = EventListMode.ARCHIVED, date = LocalDate.of(2030, 1, 3)), now))
    }

    @Test
    fun expiredItemsRemainTodoWhileArchiveTakesPrecedenceOverCompletion() {
        val overdue = event("overdue", "2030-01-02T15:00:00Z")
        val completed = event("completed").copy(completedAt = 1)
        val archived = event("archived").copy(archivedAt = 2)
        val both = event("both").copy(completedAt = 1, archivedAt = 2)
        val rows = listOf(overdue, completed, archived, both)
        assertEquals(listOf(overdue), EventQuery.select(rows, EventFilter(), now))
        assertEquals(listOf(completed), EventQuery.select(rows, EventFilter(mode = EventListMode.COMPLETED), now))
        assertEquals(listOf(archived, both), EventQuery.select(rows, EventFilter(mode = EventListMode.ARCHIVED), now))
        assertTrue(EventQuery.select(rows, EventFilter(dangerOnly = true), now).isEmpty())
    }

    @Test
    fun filteringReturnsStableDeadlineThenIdOrderWithoutChangingInput() {
        val rows = listOf(event("b"), event("z", "2030-01-02T15:01:00Z"), event("a"))
        assertEquals(listOf("z", "a", "b"), EventQuery.select(rows, EventFilter(), now).map { it.id })
        assertEquals(listOf("b", "z", "a"), rows.map { it.id })
    }
}
