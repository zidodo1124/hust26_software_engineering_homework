package cn.edu.hust.ddlsaver.platform

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.CRC32

/** Exercises the real decoder and private copies, not mocked attachment metadata. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AttachmentStoreTest {
    private lateinit var context: Context
    private lateinit var sources: File
    private lateinit var copies: File
    private lateinit var store: AttachmentStore

    @Before
    fun prepare() {
        context = ApplicationProvider.getApplicationContext()
        sources = File(context.cacheDir, "attachment-test-${UUID.randomUUID()}").apply { mkdirs() }
        copies = File(context.filesDir, "attachments")
        copies.deleteRecursively()
        store = AttachmentStore(context)
    }

    @After
    fun removeOwnedTestFiles() {
        sources.deleteRecursively()
        copies.deleteRecursively()
    }

    @Test
    fun importCopiesMultipleRealImagesAndRetainsSources() = runBlocking {
        val first = png("课程.png", width = 36, height = 24)
        val second = png("进度.png", width = 11, height = 19)
        val originalBytes = first.readBytes()

        val refs = store.importImages(listOf(Uri.fromFile(first), Uri.fromFile(second)))

        assertEquals(2, refs.size)
        assertEquals(listOf("课程.png", "进度.png"), refs.map { it.displayName })
        assertEquals(36, refs[0].width)
        assertEquals(24, refs[0].height)
        assertEquals("image/png", refs[0].mimeType)
        assertEquals(first.length(), refs[0].byteSize)
        assertArrayEquals(originalBytes, File(copies, refs[0].relativePath).readBytes())
        assertArrayEquals(originalBytes, first.readBytes())
        assertTrue(second.isFile)
        assertTrue(refs.all { it.relativePath == "${it.id}.img" })
        assertEquals(2, copies.listFiles().orEmpty().size)
        val restoredPreview = AttachmentStore(context).loadPreview(refs[0])
        assertEquals(36, restoredPreview.width)
        restoredPreview.recycle()
    }

    @Test
    fun unreadableSecondImageRollsBackTheWholeBatchButKeepsExistingCopies() = runBlocking {
        val existing = store.importImages(listOf(Uri.fromFile(png("existing.png")))).single()
        val first = png("new.png")
        val bad = File(sources, "broken.png").apply { writeText("This is not a PNG image.") }

        expectAttachmentFailure { store.importImages(listOf(Uri.fromFile(first), Uri.fromFile(bad))) }

        assertEquals(listOf(existing.relativePath), copies.listFiles().orEmpty().map { it.name })
        assertTrue(first.isFile)
        assertEquals("This is not a PNG image.", bad.readText())
    }

    @Test
    fun cancellationDuringCopyRollsBackBothCompletedAndTemporaryCopies() = runBlocking {
        val first = png("first.png")
        val originalBytes = first.readBytes()
        val uri = Uri.parse("content://attachment-tests/cancelled-image")
        val importingJob = AtomicReference<Job>()
        shadowOf(context.contentResolver).registerInputStream(uri, object : ByteArrayInputStream(originalBytes) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                importingJob.get().cancel(CancellationException("cancel the draft during a copy"))
                return super.read(buffer, offset, length)
            }
        })

        supervisorScope {
            val task = async(Dispatchers.Default) {
                importingJob.set(currentCoroutineContext()[Job]!!)
                store.importImages(listOf(Uri.fromFile(first), uri))
            }
            try {
                task.await()
                throw AssertionError("Import should have been cancelled")
            } catch (_: CancellationException) {
                // Cancellation must finish rollback before propagating to the caller.
            }
        }

        assertTrue(copies.listFiles().orEmpty().isEmpty())
        assertArrayEquals(originalBytes, first.readBytes())
    }

    @Test
    fun permissionFailureAfterOneImageLeavesNoNewCopies() = runBlocking {
        val first = png("first.png")
        val deniedUri = Uri.parse("content://attachment-tests/permission-revoked")
        shadowOf(context.contentResolver).registerInputStream(deniedUri, object : InputStream() {
            override fun read(): Int = throw SecurityException("URI permission revoked")
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                throw SecurityException("URI permission revoked")
        })

        val failure = expectAttachmentFailure {
            store.importImages(listOf(Uri.fromFile(first), deniedUri))
        }

        assertTrue(failure.message.orEmpty().contains("图片没加上"))
        assertTrue(copies.listFiles().orEmpty().isEmpty())
        assertTrue(first.exists())
    }

    @Test
    fun declaredNonImageAndPlainWebAddressAreRejected() = runBlocking {
        val disguised = png("not-an-image.txt")

        expectAttachmentFailure { store.importImages(listOf(Uri.fromFile(disguised))) }
        val addressFailure = expectAttachmentFailure {
            store.importImages(listOf(Uri.parse("https://example.test/image.png")))
        }

        assertTrue(addressFailure.message.orEmpty().contains("网址"))
        assertTrue(copies.listFiles().orEmpty().isEmpty())
        assertTrue(disguised.isFile)
    }

    @Test
    fun oversizedImageIsStoppedByStreamingByteLimit() = runBlocking {
        val oversized = File(sources, "large.png")
        RandomAccessFile(oversized, "rw").use { it.setLength(AttachmentStore.MAX_IMAGE_BYTES + 1) }

        val failure = expectAttachmentFailure { store.importImages(listOf(Uri.fromFile(oversized))) }

        assertTrue(failure.message.orEmpty().contains("20 MiB"))
        assertTrue(copies.listFiles().orEmpty().isEmpty())
        assertEquals(AttachmentStore.MAX_IMAGE_BYTES + 1, oversized.length())
    }

    @Test
    fun extremePixelDimensionsAreRejectedWithoutAllocatingTheFullImage() = runBlocking {
        val oversized = png("huge-dimensions.png")
        val bytes = oversized.readBytes()
        // PNG IHDR dimensions and CRC: leave compressed pixels tiny to avoid allocating a huge fixture.
        ByteBuffer.wrap(bytes, 16, 8).putInt(10_001).putInt(10_001)
        val checksum = CRC32().apply { update(bytes, 12, 17) }.value
        ByteBuffer.wrap(bytes, 29, 4).putInt(checksum.toInt())
        oversized.writeBytes(bytes)

        val failure = expectAttachmentFailure { store.importImages(listOf(Uri.fromFile(oversized))) }

        assertTrue(failure.message.orEmpty().contains("1 亿"))
        assertTrue(copies.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun plausibleImageHeaderWithoutPixelsIsRejected() = runBlocking {
        val broken = png("truncated.png")
        broken.writeBytes(broken.readBytes().copyOf(33))

        expectAttachmentFailure { store.importImages(listOf(Uri.fromFile(broken))) }

        assertTrue(copies.listFiles().orEmpty().isEmpty())
        assertTrue(broken.isFile)
    }

    @Test
    fun discardingDraftKeepsReferencedCopiesAndNeverTouchesSources() = runBlocking {
        val source = png("source.png")
        val refs = store.importImages(listOf(Uri.fromFile(source), Uri.fromFile(source)))

        store.discardUnreferenced(refs, setOf(refs[0].relativePath))
        store.discardUnreferenced(refs, setOf(refs[0].relativePath)) // Repeated cleanup is safe.

        assertTrue(File(copies, refs[0].relativePath).isFile)
        assertFalse(File(copies, refs[1].relativePath).exists())
        assertTrue(source.isFile)
    }

    @Test
    fun traversalIsRejectedBeforeAnyCandidateCanBeDeleted() = runBlocking {
        val existing = store.importImages(listOf(Uri.fromFile(png("saved.png")))).single()
        val outside = File(context.filesDir, "keep-outside.png").apply { writeText("keep me") }
        try {
            val malicious = existing.copy(relativePath = "../${outside.name}")

            expectAttachmentFailure { store.discardUnreferenced(listOf(existing, malicious), emptySet()) }
            expectAttachmentFailure { store.loadPreview(malicious) }

            assertTrue(File(copies, existing.relativePath).isFile)
            assertEquals("keep me", outside.readText())
        } finally {
            outside.delete()
        }
    }

    @Test
    fun startupCleanupRetainsFreshReferencedProtectedAndUnknownFiles() = runBlocking {
        val source = png("source.png")
        val refs = store.importImages(List(4) { Uri.fromFile(source) })
        val oldTimestamp = System.currentTimeMillis() - 2 * 86_400_000L
        refs.take(3).forEach { assertTrue(File(copies, it.relativePath).setLastModified(oldTimestamp)) }
        val temporary = File(copies, "${UUID.randomUUID()}.part").apply {
            writeText("interrupted old import")
            assertTrue(setLastModified(oldTimestamp))
        }
        val unknown = File(copies, "keep-unrelated.txt").apply {
            writeText("not managed by AttachmentStore")
            assertTrue(setLastModified(oldTimestamp))
        }

        store.cleanupOrphans(setOf(refs[0].relativePath), setOf(refs[1].relativePath))

        assertTrue(File(copies, refs[0].relativePath).isFile)
        assertTrue(File(copies, refs[1].relativePath).isFile)
        assertFalse(File(copies, refs[2].relativePath).exists())
        assertTrue(File(copies, refs[3].relativePath).isFile)
        assertFalse(temporary.exists())
        assertTrue(unknown.isFile)
        assertTrue(source.isFile)
    }

    @Test
    fun previewsAreSampledAndHardClampedTo1600Pixels() = runBlocking {
        val ref = store.importImages(listOf(Uri.fromFile(png("wide.png", 3_200, 1_600)))).single()

        val thumbnail = store.loadPreview(ref, 320)
        val fullPreview = store.loadPreview(ref, 10_000)

        assertTrue(thumbnail.width <= 320 && thumbnail.height <= 320)
        assertTrue(thumbnail.width > 0 && thumbnail.height > 0)
        assertEquals(2, thumbnail.width / thumbnail.height)
        assertTrue(fullPreview.width <= 1_600 && fullPreview.height <= 1_600)
        assertTrue(fullPreview.width > thumbnail.width)
        assertEquals(3_200, ref.width)
        thumbnail.recycle()
        fullPreview.recycle()
    }

    @Test
    fun missingCopyHasVisibleErrorAndDoesNotChangeOtherAttachments() = runBlocking {
        val refs = store.importImages(List(2) { Uri.fromFile(png("source-$it.png")) })
        assertTrue(File(copies, refs[0].relativePath).delete())

        val failure = expectAttachmentFailure { store.loadPreview(refs[0]) }

        assertTrue(failure.message.orEmpty().contains("已经不在了"))
        assertTrue(File(copies, refs[1].relativePath).isFile)
    }

    private fun png(name: String, width: Int = 24, height: Int = 16): File {
        val file = File(sources, name)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xff537a89.toInt())
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        return file
    }

    private suspend fun expectAttachmentFailure(block: suspend () -> Any?): AttachmentException {
        try {
            block()
        } catch (failure: AttachmentException) {
            return failure
        }
        throw AssertionError("Expected a user-readable AttachmentException")
    }
}
