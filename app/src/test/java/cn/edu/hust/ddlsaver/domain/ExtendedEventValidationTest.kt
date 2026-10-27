package cn.edu.hust.ddlsaver.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ExtendedEventValidationTest {
    private fun draft() = EventDraft(title = "报告", deadlineAt = 1_893_456_000_000)
    private fun attachment(): AttachmentRef {
        val id = UUID.randomUUID().toString()
        return AttachmentRef(id, "$id.img", "image/png", "截图.png", 1024, 20, 30)
    }

    @Test
    fun customFieldsPreserveValueAndOrderAndNormalizeNames() {
        val fields = listOf(CustomField("second", " 地点 ", " 东九 "), CustomField("first", "网址", "https://example.org"))
        val result = EventValidation.normalize(draft().copy(customFields = fields)).customFields
        assertEquals(listOf("second", "first"), result.map { it.id })
        assertEquals("地点", result.first().label)
        assertEquals(" 东九 ", result.first().value)
    }

    @Test
    fun emptyFieldNamesAndDuplicateFieldIdsAreRejected() {
        listOf(
            listOf(CustomField("one", " ", "value")),
            listOf(CustomField("one", "地点", "A"), CustomField("one", "联系人", "B")),
        ).forEach { fields ->
            assertTrue(runCatching { EventValidation.normalize(draft().copy(customFields = fields)) }
                .exceptionOrNull() is EventValidationException)
        }
    }

    @Test
    fun attachmentMustUseSafeGeneratedFilenameAndValidImageMetadata() {
        val image = attachment()
        assertEquals(image, EventValidation.normalize(draft().copy(attachments = listOf(image))).attachments.single())
        val invalid = listOf(
            image.copy(relativePath = "../${image.relativePath}"),
            image.copy(relativePath = "C:\\temp\\image.img"),
            image.copy(relativePath = "arbitrary.img"),
            image.copy(id = UUID.randomUUID().toString()),
            image.copy(mimeType = "text/plain"), image.copy(byteSize = -1), image.copy(width = -1),
        )
        invalid.forEach { ref ->
            assertTrue(runCatching { EventValidation.normalize(draft().copy(attachments = listOf(ref))) }
                .exceptionOrNull() is EventValidationException)
        }
    }

    @Test
    fun duplicateAttachmentIdsAndPathsAreRejectedBeforeSaving() {
        val image = attachment()
        listOf(image, image.copy(relativePath = "${UUID.randomUUID()}.img"), image.copy(id = "other-id")).forEach { duplicate ->
            assertTrue(runCatching { EventValidation.normalize(draft().copy(attachments = listOf(image, duplicate))) }
                .exceptionOrNull() is EventValidationException)
        }
    }
}
