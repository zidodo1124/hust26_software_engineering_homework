package cn.edu.hust.ddlsaver.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class CharacterFeedbackTest {
    private val now = Instant.parse("2030-01-02T10:00:00Z")
    private val settings = AppSettings()
    private fun event(id: String = "a", secondsLeft: Long = 172800, sprint: Long? = null) = DdlEvent(
        id, "测试事项", now.plusSeconds(secondsLeft).toEpochMilli(), sprint, "", Importance.NORMAL,
        null, null, 0, 0, 1,
    )

    @Test fun emptyListHasNormalTextAndDefaultArtwork() {
        val feedback = selectCharacterFeedback(emptyList(), now, settings)
        assertEquals(CharacterState.NORMAL, feedback.state)
        assertEquals("暂时没有待办", feedback.title)
        assertEquals("character/maid-left.webp", feedback.imageAsset)
        assertNull(feedback.focusEventId)
        assertTrue(feedback.line.isNotBlank())
    }

    @Test fun explicitSuccessfulCompletionOverridesUrgencyButOldCompletedRowsNeverDo() {
        val danger = event(secondsLeft = 60, sprint = 2)
        assertEquals(CharacterState.COMPLETED, selectCharacterFeedback(listOf(danger), now, settings, true).state)
        val finished = danger.copy(completedAt = now.toEpochMilli())
        assertEquals("暂时没有待办", selectCharacterFeedback(listOf(finished), now, settings).title)
        assertEquals(CharacterState.NORMAL, selectCharacterFeedback(listOf(finished), now, settings).state)
    }

    @Test fun archivedAndCompletedItemsCannotDriveDangerOrOverdue() {
        val excluded = listOf(event("done", -60).copy(completedAt = 1),
            event("archive", 60, 2).copy(archivedAt = 1))
        val normal = event("active")
        val feedback = selectCharacterFeedback(excluded + normal, now, settings)
        assertEquals(CharacterState.NORMAL, feedback.state)
        assertEquals(normal.id, feedback.focusEventId)
    }

    @Test fun actionableDangerTakesPriorityThenOverdueThenSoon() {
        val danger = event("danger", 60, 2)
        val overdue = event("overdue", -1)
        val soon = event("soon", 3600)
        assertEquals(CharacterState.DANGER, selectCharacterFeedback(listOf(soon, overdue, danger), now, settings).state)
        assertEquals(CharacterState.OVERDUE, selectCharacterFeedback(listOf(soon, overdue), now, settings).state)
        assertEquals(CharacterState.SOON, selectCharacterFeedback(listOf(soon), now, settings).state)
    }

    @Test fun sprintAndDeadlineBoundariesReuseUrgencyRules() {
        val item = event(secondsLeft = 120, sprint = 2)
        assertEquals(CharacterState.SOON, selectCharacterFeedback(listOf(item), now.minusSeconds(1), settings).state)
        assertEquals(CharacterState.DANGER, selectCharacterFeedback(listOf(item), now, settings).state)
        assertEquals(CharacterState.OVERDUE, selectCharacterFeedback(listOf(item), now.plusSeconds(120), settings).state)
    }

    @Test fun configurableThresholdAndAbsentSprintDoNotInventDanger() {
        val item = event(secondsLeft = 2 * 3600)
        assertEquals(CharacterState.NORMAL, selectCharacterFeedback(listOf(item), now, settings.copy(urgentMinutes = 60)).state)
        assertEquals(CharacterState.SOON, selectCharacterFeedback(listOf(item), now, settings.copy(urgentMinutes = 180)).state)
        assertEquals(CharacterState.SOON, selectCharacterFeedback(listOf(item.copy(deadlineAt = now.plusSeconds(1).toEpochMilli())), now, settings).state)
    }

    @Test fun focusAndDialogueAreIndependentFromInputOrderAndImportance() {
        val a = event("a", 3600)
        val b = event("b", 3600)
        val forward = selectCharacterFeedback(listOf(a, b), now, settings)
        assertEquals(forward, selectCharacterFeedback(listOf(b, a), now, settings))
        assertEquals("a", forward.focusEventId)
        assertEquals(forward, selectCharacterFeedback(listOf(a.copy(importance = Importance.HIGH), b), now, settings))
    }

    @Test fun dialogueIsStableWithinTimeBucketAndVariesWithoutRandomness() {
        val item = event(secondsLeft = 10 * 86400)
        val feedback = selectCharacterFeedback(listOf(item), now, settings)
        assertEquals(feedback, selectCharacterFeedback(listOf(item), now.plusSeconds(30), settings))
        val lines = (0..5).map { selectCharacterFeedback(listOf(item), now.plusSeconds(it * 600L), settings).line }
        assertEquals(6, lines.toSet().size)
        assertTrue(lines.all(String::isNotBlank))
    }

    @Test fun everyStatusUsesExpectedExpressionAndReadableText() {
        val cases = listOf(
            event(secondsLeft = 10 * 86400) to "maid-left.webp",
            event(secondsLeft = 3600) to "whale-serious.webp",
            event(secondsLeft = 60, sprint = 2) to "whale-angry.webp",
            event(secondsLeft = -1) to "whale-serious.webp",
        )
        cases.forEach { (item, file) ->
            val lines = (0..5).map { selectCharacterFeedback(listOf(item), now.plusSeconds(it * 600L), settings) }
            // Danger crosses its deadline in this time range; check its immediate expression separately.
            val immediate = selectCharacterFeedback(listOf(item), now, settings)
            assertEquals("character/$file", immediate.imageAsset)
            assertTrue(immediate.imageDescription.isNotBlank())
            assertTrue(lines.all { it.title.isNotBlank() && it.line.isNotBlank() })
        }
        assertEquals("character/whale-cheerful.webp", selectCharacterFeedback(emptyList(), now, settings, true).imageAsset)
    }
}
