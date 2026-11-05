package cn.edu.hust.ddlsaver.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import cn.edu.hust.ddlsaver.domain.AttachmentRef
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.URLConnection
import java.nio.file.Files
import java.util.UUID

class AttachmentException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** Immutable application-owned copies. This class never modifies or deletes source URIs. */
class AttachmentStore(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val root = File(appContext.filesDir, "attachments")

    suspend fun importImages(uris: List<Uri>): List<AttachmentRef> {
        if (uris.isEmpty()) return emptyList()
        // Keep ownership outside withContext: cancellation while returning from IO must also roll back.
        val created = mutableListOf<File>()
        val diagnostics = AttachmentImportDiagnostics(appContext, uris.size)
        try {
            return withContext(Dispatchers.IO) {
                fileOperations.withLock {
                    ensureDirectory()
                    uris.mapIndexed { index, uri ->
                        currentCoroutineContext().ensureActive()
                        diagnostics.startItem(index, uri.scheme)
                        importOne(uri, created, diagnostics)
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            rollback(created, cancelled)
            throw cancelled
        } catch (failure: Exception) {
            val suppressedBeforeRollback = failure.suppressed.size
            rollback(created, failure)
            diagnostics.reportFailure(failure, failure.suppressed.size - suppressedBeforeRollback)
            throw if (failure is AttachmentException) failure
            else {
                // A denied private-file operation must not be presented as a source URI permission issue.
                val sourceAccessDenied = failure is SecurityException && diagnostics.stage in sourceAccessStages
                val message = if (sourceAccessDenied) {
                    "图片没加上：没有读取这张图片的权限。请重新复制图片，或点“从相册选择”再添加。"
                } else "图片没加上，请检查图片能否打开，以及手机存储空间是否够用。"
                AttachmentException(message, failure)
            }
        }
    }

    suspend fun discardUnreferenced(candidates: List<AttachmentRef>, referencedPaths: Set<String>) {
        withContext(Dispatchers.IO) {
            fileOperations.withLock {
                ensureDirectory()
                // Validate the entire request before deleting anything.
                val files = candidates.distinctBy { it.relativePath }.map { ref ->
                    validateReference(ref)
                    checkedFile(ref.relativePath)
                }
                files.filterNot { it.name in referencedPaths }.forEach(::deleteOwnedFile)
            }
        }
    }

    suspend fun cleanupOrphans(
        referencedPaths: Set<String>,
        protectedPaths: Set<String> = emptySet(),
        minAgeMillis: Long = 86_400_000L,
    ) {
        require(minAgeMillis >= 0) { "清理等待时间不能为负数。" }
        withContext(Dispatchers.IO) {
            fileOperations.withLock {
                ensureDirectory()
                val now = System.currentTimeMillis()
                root.listFiles().orEmpty().filter { candidate ->
                    ownedName.matches(candidate.name) && candidate.isFile &&
                        !Files.isSymbolicLink(candidate.toPath()) &&
                        candidate.name !in referencedPaths && candidate.name !in protectedPaths &&
                        candidate.lastModified() <= now && now - candidate.lastModified() >= minAgeMillis
                }.forEach { deleteOwnedFile(checkedFile(it.name, allowTemporary = true)) }
            }
        }
    }

    suspend fun loadPreview(ref: AttachmentRef, maxDimension: Int = 1600): Bitmap =
        withContext(Dispatchers.IO) {
            fileOperations.withLock {
                try {
                    validateReference(ref)
                    if (maxDimension <= 0) throw AttachmentException("图片预览尺寸无效。")
                    ensureDirectory()
                    val file = checkedFile(ref.relativePath)
                    if (!file.isFile) throw AttachmentException("保存的图片已经不在了，请重新添加。")
                    val bounds = readBounds(file)
                    val limit = maxDimension.coerceAtMost(MAX_PREVIEW_DIMENSION)
                    var sample = 1
                    while ((bounds.outWidth + sample - 1) / sample > limit ||
                        (bounds.outHeight + sample - 1) / sample > limit
                    ) sample *= 2
                    currentCoroutineContext().ensureActive()
                    val bitmap = decodePrivateImage(
                        file,
                        BitmapFactory.Options().apply { inSampleSize = sample },
                    ) ?: throw AttachmentException("这张图片打不开，请重新添加能正常打开的图片。")
                    orient(bitmap, file)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: AttachmentException) {
                    throw failure
                } catch (failure: Exception) {
                    throw AttachmentException("这张图片暂时打不开，请重新添加后再试。", failure)
                } catch (failure: OutOfMemoryError) {
                    throw AttachmentException("内存不够，暂时看不了这张图片。", failure)
                }
            }
        }

    private suspend fun importOne(
        uri: Uri,
        created: MutableList<File>,
        diagnostics: AttachmentImportDiagnostics,
    ): AttachmentRef {
        if (uri.scheme != "content" && uri.scheme != "file") {
            throw AttachmentException("图片网址不能直接添加，请选择或粘贴图片本身。")
        }
        val declaredType = if (uri.scheme == "content") resolver.getType(uri)
        else URLConnection.guessContentTypeFromName(uri.lastPathSegment)
        if (declaredType != null && declaredType != "application/octet-stream" &&
            !declaredType.startsWith("image/", ignoreCase = true)
        ) throw AttachmentException("选中的内容不是图片，请重新选择。")

        diagnostics.stage = AttachmentImportDiagnostics.Stage.CREATE_COPY
        val id = UUID.randomUUID().toString()
        val temporary = checkedFile("$id.part", allowTemporary = true)
        val destination = checkedFile("$id.img")
        if (!temporary.createNewFile()) throw AttachmentException("没能创建图片副本，请再试一次。")
        created += temporary
        var byteCount = 0L
        diagnostics.stage = AttachmentImportDiagnostics.Stage.OPEN_SOURCE
        val source = resolver.openInputStream(uri)
            ?: throw AttachmentException("读不到这张图片，请重新选择并允许访问。")
        source.use { input ->
            diagnostics.stage = AttachmentImportDiagnostics.Stage.WRITE_COPY
            temporary.outputStream().buffered().use { output ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    diagnostics.stage = AttachmentImportDiagnostics.Stage.READ_SOURCE
                    val count = input.read(buffer)
                    if (count < 0) {
                        diagnostics.stage = AttachmentImportDiagnostics.Stage.WRITE_COPY
                        break
                    }
                    if (count == 0) continue
                    byteCount += count
                    if (byteCount > MAX_IMAGE_BYTES) {
                        throw AttachmentException("这张图片超过了 20 MiB，请压缩后再添加。")
                    }
                    diagnostics.stage = AttachmentImportDiagnostics.Stage.WRITE_COPY
                    output.write(buffer, 0, count)
                }
            }
            diagnostics.stage = AttachmentImportDiagnostics.Stage.CLOSE_SOURCE
        }
        diagnostics.stage = AttachmentImportDiagnostics.Stage.DECODE_IMAGE
        val bounds = readBounds(temporary)
        // A plausible header is insufficient: decode a bounded thumbnail before accepting the file.
        var validationSample = 1
        while ((bounds.outWidth + validationSample - 1) / validationSample > 128 ||
            (bounds.outHeight + validationSample - 1) / validationSample > 128
        ) validationSample *= 2
        val validationBitmap = decodePrivateImage(
            temporary,
            BitmapFactory.Options().apply { inSampleSize = validationSample },
        ) ?: throw AttachmentException("这张图片读不出来，可能已损坏，请换一张。")
        validationBitmap.recycle()
        currentCoroutineContext().ensureActive()
        diagnostics.stage = AttachmentImportDiagnostics.Stage.COMMIT_COPY
        if (destination.exists()) throw AttachmentException("保存图片时编号重复了，请再试一次。")
        created += destination
        if (!temporary.renameTo(destination)) throw AttachmentException("图片没保存下来，请检查手机存储空间。")
        return AttachmentRef(
            id = id,
            relativePath = destination.name,
            mimeType = requireNotNull(bounds.outMimeType),
            displayName = displayName(uri),
            byteSize = byteCount,
            width = bounds.outWidth,
            height = bounds.outHeight,
        )
    }

    private fun readBounds(file: File): BitmapFactory.Options {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decodePrivateImage(file, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
            bounds.outMimeType?.startsWith("image/") != true
        ) throw AttachmentException("读不出图片内容，可能是格式不支持或文件已损坏。")
        if (bounds.outWidth.toLong() * bounds.outHeight > MAX_IMAGE_PIXELS) {
            throw AttachmentException("这张图片超过 1 亿像素，请缩小尺寸后再添加。")
        }
        return bounds
    }

    // Own the stream lifetime explicitly: no decoder may keep a file open across rename/cleanup.
    private fun decodePrivateImage(file: File, options: BitmapFactory.Options): Bitmap? =
        file.inputStream().use { input -> BitmapFactory.decodeStream(input, null, options) }

    private fun displayName(uri: Uri): String {
        val queried = if (uri.scheme == "content") {
            try {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            } catch (_: Exception) {
                null // A missing display name does not invalidate successfully copied image content.
            }
        } else File(uri.path.orEmpty()).name
        return queried?.replace(Regex("[\\p{Cntrl}]"), " ")?.trim()?.take(160)
            ?.takeIf { it.isNotEmpty() } ?: "图片附件"
    }

    private fun validateReference(ref: AttachmentRef) {
        if (!imageName.matches(ref.relativePath) || ref.relativePath != "${ref.id}.img") {
            throw AttachmentException("图片信息有误，请重新添加。")
        }
    }

    private fun ensureDirectory() {
        if (Files.isSymbolicLink(root.toPath()) || root.canonicalFile.parentFile != appContext.filesDir.canonicalFile) {
            throw AttachmentException("图片存储目录有问题，暂时无法使用。")
        }
        if (!root.isDirectory && !root.mkdirs()) throw AttachmentException("没能创建图片存储目录，请检查手机存储空间。")
    }

    private fun checkedFile(name: String, allowTemporary: Boolean = false): File {
        if (!(if (allowTemporary) ownedName else imageName).matches(name)) {
            throw AttachmentException("图片保存位置有误，请重新添加。")
        }
        val file = File(root, name)
        if (Files.isSymbolicLink(file.toPath()) || file.canonicalFile.parentFile != root.canonicalFile) {
            throw AttachmentException("图片路径超出应用存储范围。")
        }
        return file
    }

    private fun deleteOwnedFile(file: File) {
        if (file.exists() && !file.delete()) {
            throw AttachmentException("没用到的图片副本还没清理完，下次打开时会重试。")
        }
    }

    private suspend fun rollback(created: List<File>, original: Throwable) {
        withContext(NonCancellable + Dispatchers.IO) {
            fileOperations.withLock {
                created.forEach { file ->
                    try {
                        deleteOwnedFile(checkedFile(file.name, allowTemporary = true))
                    } catch (cleanupFailure: Exception) {
                        original.addSuppressed(cleanupFailure)
                    }
                }
            }
        }
    }

    private fun orient(bitmap: Bitmap, file: File): Bitmap {
        val orientation = try {
            ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } catch (_: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.setRotate(270f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
        }
        if (matrix.isIdentity) return bitmap
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    companion object {
        const val MAX_IMAGE_BYTES = 20L * 1024 * 1024
        const val MAX_IMAGE_PIXELS = 100_000_000L
        const val MAX_PREVIEW_DIMENSION = 1600
        private const val UUID_PATTERN = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
        private val imageName = Regex("$UUID_PATTERN\\.img")
        private val ownedName = Regex("$UUID_PATTERN\\.(img|part)")
        private val fileOperations = Mutex()
        private val sourceAccessStages = setOf(
            AttachmentImportDiagnostics.Stage.METADATA,
            AttachmentImportDiagnostics.Stage.OPEN_SOURCE,
            AttachmentImportDiagnostics.Stage.READ_SOURCE,
            AttachmentImportDiagnostics.Stage.CLOSE_SOURCE,
        )
    }
}
