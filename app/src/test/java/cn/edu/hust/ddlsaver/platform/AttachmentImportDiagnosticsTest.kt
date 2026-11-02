package cn.edu.hust.ddlsaver.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLog
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Real import failures exercise stage reporting; no claims about device URI grant lifetime. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AttachmentImportDiagnosticsTest {
    private lateinit var context: Context
    private lateinit var sources: File
    private lateinit var copies: File
    private lateinit var store: AttachmentStore
    private var originalFlags = 0

    @Before
    fun prepare() {
        context = ApplicationProvider.getApplicationContext()
        originalFlags = context.applicationInfo.flags
        context.applicationInfo.flags = originalFlags or ApplicationInfo.FLAG_DEBUGGABLE
        sources = File(context.cacheDir, "import-diagnostic-sources").apply { mkdirs() }
        copies = File(context.filesDir, "attachments")
        copies.deleteRecursively()
        store = AttachmentStore(context)
        ShadowLog.clear()
    }

    @After
    fun cleanUp() {
        context.applicationInfo.flags = originalFlags
        ShadowLog.reset()
        sources.deleteRecursively()
        copies.deleteRecursively()
    }

    @Test
    fun deniedSecondSourceReportsOnlyAllowlistedFieldsAfterBatchRollback() = runBlocking {
        val existingSource = png("private-existing-name.png")
        val existingBytes = existingSource.readBytes()
        val existing = store.importImages(listOf(Uri.fromFile(existingSource))).single()
        val freshSource = png("private-new-name.png")
        val denied = Uri.parse("content://private-authority/secret-picture?token=secret-token")
        val original = SecurityException("private message: $denied ${freshSource.absolutePath}")
        original.addSuppressed(IOException("private provider cleanup message"))
        registerFailure(denied, original)

        val failure = expectFailure { store.importImages(listOf(Uri.fromFile(freshSource), denied)) }

        assertOriginalCause(original, failure)
        assertEquals("图片导入失败，请确认图片可读取且本机存储空间充足。", failure.message)
        assertEquals(setOf(existing.relativePath), privateNames())
        assertArrayEquals(existingBytes, File(copies, existing.relativePath).readBytes())
        assertTrue(freshSource.isFile)
        assertLog("read_source", "content", 2, 2, "security_denied")
    }

    @Test
    fun missingSourceIsReportedAtOpenWithoutLoggingItsPath() = runBlocking {
        val missing = File(sources, "private-missing-image.png")

        expectFailure { store.importImages(listOf(Uri.fromFile(missing))) }

        assertTrue(privateNames().isEmpty())
        assertLog("open_source", "file", 1, 1, "not_found")
    }

    @Test
    fun invalidImageReportsDecodeValidationWithoutLoggingImageContents() = runBlocking {
        val bad = File(sources, "private-invalid-image.png").apply { writeText("private image content") }

        expectFailure { store.importImages(listOf(Uri.fromFile(bad))) }

        assertTrue(privateNames().isEmpty())
        assertEquals("private image content", bad.readText())
        assertLog("decode_image", "file", 1, 1, "validation")
    }

    @Test
    fun unusablePrivateDirectoryIsDistinctFromSourceReadFailure() = runBlocking {
        copies.writeText("not a directory")
        val source = png("valid.png")

        expectFailure { store.importImages(listOf(Uri.fromFile(source))) }

        assertEquals("not a directory", copies.readText())
        assertTrue(source.isFile)
        assertLog("ensure_directory", "none", 0, 1, "validation")
    }

    @Test
    fun destinationCollisionReportsCommitAndPreservesThePreexistingFile() = runBlocking {
        val source = png("source.png")
        val uri = Uri.parse("content://private-authority/commit-source")
        val conflicting = AtomicReference<File>()
        shadowOf(context.contentResolver).registerInputStream(uri, object : ByteArrayInputStream(source.readBytes()) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (conflicting.get() == null) {
                    val temporary = copies.listFiles().orEmpty().single { it.extension == "part" }
                    val destination = File(copies, temporary.nameWithoutExtension + ".img")
                    destination.writeText("preexisting file must remain")
                    conflicting.set(destination)
                }
                return super.read(buffer, offset, length)
            }
        })

        expectFailure { store.importImages(listOf(uri)) }

        assertEquals(setOf(conflicting.get().name), privateNames())
        assertEquals("preexisting file must remain", conflicting.get().readText())
        assertLog("commit_copy", "content", 1, 1, "validation")
    }

    @Test
    fun successfulImportAndCancellationDoNotEmitFailureDiagnostics() = runBlocking {
        val source = png("source.png")
        val originalBytes = source.readBytes()
        val kept = store.importImages(listOf(Uri.fromFile(source))).single()
        val uri = Uri.parse("content://private-authority/cancelled")
        val importingJob = AtomicReference<Job>()
        shadowOf(context.contentResolver).registerInputStream(uri, object : ByteArrayInputStream(originalBytes) {
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                importingJob.get().cancel(CancellationException("private cancelled draft"))
                return super.read(buffer, offset, length)
            }
        })
        supervisorScope {
            val task = async(Dispatchers.Default) {
                importingJob.set(currentCoroutineContext()[Job]!!)
                store.importImages(listOf(Uri.fromFile(source), uri))
            }
            try {
                task.await()
                throw AssertionError("Import should have been cancelled")
            } catch (_: CancellationException) {
                // The production path must finish its rollback and preserve cancellation.
            }
        }

        assertEquals(setOf(kept.relativePath), privateNames())
        assertArrayEquals(originalBytes, File(copies, kept.relativePath).readBytes())
        assertTrue(ShadowLog.getLogsForTag(AttachmentImportDiagnostics.TAG).isEmpty())
    }

    @Test
    fun nonDebuggableApplicationDoesNotLogImportFailures() = runBlocking {
        context.applicationInfo.flags = originalFlags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        val denied = Uri.parse("content://private-authority/release-image")
        val original = SecurityException("private release failure")
        registerFailure(denied, original)

        val failure = expectFailure { store.importImages(listOf(denied)) }

        assertOriginalCause(original, failure)
        assertTrue(privateNames().isEmpty())
        assertTrue(ShadowLog.getLogsForTag(AttachmentImportDiagnostics.TAG).isEmpty())
    }

    @Test
    fun failingLogBackendCannotReplaceTheOriginalImportFailure() = runBlocking {
        val denied = Uri.parse("content://private-authority/log-failure")
        val original = SecurityException("original private failure")
        registerFailure(denied, original)
        val logAttempts = AtomicInteger()
        ShadowLog.setTimeSupplier {
            logAttempts.incrementAndGet()
            throw IllegalStateException("Controlled diagnostic backend failure")
        }

        val failure = try {
            expectFailure { store.importImages(listOf(denied)) }
        } finally {
            ShadowLog.reset()
        }

        assertTrue("The log backend must actually have failed", logAttempts.get() > 0)
        assertOriginalCause(original, failure)
        assertTrue(privateNames().isEmpty())
    }

    private fun registerFailure(uri: Uri, failure: SecurityException) {
        shadowOf(context.contentResolver).registerInputStream(uri, object : InputStream() {
            override fun read(): Int = throw failure
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int = throw failure
        })
    }

    private fun assertLog(stage: String, scheme: String, item: Int, count: Int, category: String) {
        val entry = ShadowLog.getLogsForTag(AttachmentImportDiagnostics.TAG).single()
        assertEquals(Log.WARN, entry.type)
        assertEquals("import_failed stage=$stage scheme=$scheme item=$item count=$count " +
            "category=$category rollback_failures=0", entry.msg)
        assertNull("Never attach a Throwable containing source paths or URI tokens", entry.throwable)
    }

    private fun privateNames(): Set<String> = copies.listFiles().orEmpty().map { it.name }.toSet()

    private fun assertOriginalCause(original: Exception, failure: AttachmentException) {
        // Coroutine stack-trace recovery can insert a copied exception above the original cause.
        var current: Throwable? = failure.cause
        repeat(8) {
            if (current === original) return
            current = current?.cause
        }
        throw AssertionError("The original source failure must remain in the cause chain")
    }

    private fun png(name: String): File {
        val file = File(sources, name)
        val bitmap = Bitmap.createBitmap(24, 16, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(0xff537a89.toInt())
        try { file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
        finally { bitmap.recycle() }
        return file
    }

    private suspend fun expectFailure(block: suspend () -> Any?): AttachmentException {
        try { block() } catch (failure: AttachmentException) { return failure }
        throw AssertionError("Expected AttachmentException")
    }
}
