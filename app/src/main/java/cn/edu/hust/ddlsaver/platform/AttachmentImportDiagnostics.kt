package cn.edu.hust.ddlsaver.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import java.io.FileNotFoundException
import java.io.IOException

/** Per-import state only. Never retain or log a URI, file name, exception text or stack trace. */
internal class AttachmentImportDiagnostics(private val context: Context, private val batchSize: Int) {
    enum class Stage(val value: String) {
        ENSURE_DIRECTORY("ensure_directory"), METADATA("metadata"), CREATE_COPY("create_copy"),
        OPEN_SOURCE("open_source"), READ_SOURCE("read_source"), WRITE_COPY("write_copy"),
        CLOSE_SOURCE("close_source"), DECODE_IMAGE("decode_image"), COMMIT_COPY("commit_copy"),
    }

    var stage: Stage = Stage.ENSURE_DIRECTORY
    private var item = 0
    private var scheme = "none"

    fun startItem(index: Int, sourceScheme: String?) {
        item = index + 1
        scheme = when (sourceScheme) {
            "content" -> "content"
            "file" -> "file"
            else -> "other"
        }
        stage = Stage.METADATA
    }

    fun reportFailure(failure: Exception, rollbackFailures: Int) {
        try {
            if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
            val category = when (failure) {
                is SecurityException -> "security_denied"
                is FileNotFoundException -> "not_found"
                is AttachmentException -> "validation"
                is IOException -> "io"
                else -> "other"
            }
            // The two-argument overload is intentional: Throwable messages can contain private URIs.
            Log.w(TAG, "import_failed stage=${stage.value} scheme=$scheme item=$item count=$batchSize " +
                "category=$category rollback_failures=$rollbackFailures")
        } catch (_: Exception) {
            // Diagnostics must never replace the import failure or interfere with completed rollback.
        }
    }

    companion object {
        const val TAG = "DDLSaver.Attachment"
    }
}
