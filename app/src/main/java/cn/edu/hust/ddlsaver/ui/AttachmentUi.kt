package cn.edu.hust.ddlsaver.ui

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cn.edu.hust.ddlsaver.domain.AttachmentRef
import cn.edu.hust.ddlsaver.platform.AttachmentStore
import kotlinx.coroutines.CancellationException

@Composable
fun AttachmentEditor(
    attachments: List<AttachmentRef>,
    busy: Boolean,
    onImportUris: (List<Uri>) -> Unit,
    onRemove: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) onImportUris(uris)
    }
    Column(Modifier.fillMaxWidth().testTag("attachment-editor"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("图片附件 · ${attachments.size} 张", fontWeight = FontWeight.SemiBold)
        Text("支持相册多选或粘贴图片。单张不超过 20 MiB、1 亿像素。", color = Muted, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                modifier = Modifier.weight(1f).testTag("attachment-pick"),
                enabled = !busy,
                onClick = {
                    try {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    } catch (_: Exception) {
                        onError("当前设备无法打开图片选择器，请检查系统相册或文件应用。")
                    }
                },
            ) { Text("从相册选择") }
            OutlinedButton(
                modifier = Modifier.weight(1f).testTag("attachment-paste"),
                enabled = !busy,
                onClick = {
                    try {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        val uris = if (clip == null) emptyList() else (0 until clip.itemCount).mapNotNull { index ->
                            clip.getItemAt(index).uri?.takeIf { it.scheme == "content" || it.scheme == "file" }
                        }
                        if (uris.isEmpty()) {
                            onError("剪贴板中没有可读取的图片。请先在支持复制图片的应用中复制；图片网址不能直接导入。")
                        } else onImportUris(uris)
                    } catch (_: Exception) {
                        onError("无法读取剪贴板图片，请重新复制或从相册选择。")
                    }
                },
            ) { Text("粘贴图片") }
        }
        if (attachments.isNotEmpty()) AttachmentStrip(attachments, busy, onRemove)
    }
}

@Composable
fun AttachmentGallery(attachments: List<AttachmentRef>) {
    Column(Modifier.fillMaxWidth().testTag("attachment-gallery"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("图片附件 · ${attachments.size} 张", fontWeight = FontWeight.SemiBold)
        if (attachments.isEmpty()) {
            Text("暂未添加图片。", color = Muted, style = MaterialTheme.typography.bodySmall)
        } else AttachmentStrip(attachments)
    }
}

@Composable
private fun AttachmentStrip(
    attachments: List<AttachmentRef>,
    busy: Boolean = false,
    onRemove: ((String) -> Unit)? = null,
) {
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    LazyRow(
        modifier = Modifier.fillMaxWidth().testTag("attachment-strip"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(attachments, key = { it.id }) { attachment ->
            Column(Modifier.width(112.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(112.dp).testTag("attachment-preview-${attachment.id}")
                        .clickable { selectedId = attachment.id },
                ) {
                    Preview(attachment, maxDimension = 320, contentScale = ContentScale.Crop)
                }
                Text(attachment.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                onRemove?.let { remove ->
                    TextButton(
                        modifier = Modifier.fillMaxWidth().testTag("attachment-remove-${attachment.id}"),
                        enabled = !busy,
                        onClick = { remove(attachment.id) },
                    ) { Text("移除") }
                }
            }
        }
    }
    val index = attachments.indexOfFirst { it.id == selectedId }
    LaunchedEffect(attachments, selectedId) {
        if (selectedId != null && index < 0) selectedId = null
    }
    if (index >= 0) {
        Dialog(onDismissRequest = { selectedId = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(color = Color.White, modifier = Modifier.fillMaxSize().padding(WindowInsets.systemBars.asPaddingValues())) {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("第 ${index + 1} / ${attachments.size} 张", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        TextButton(onClick = { selectedId = null }, modifier = Modifier.testTag("attachment-viewer-close")) { Text("关闭") }
                    }
                    Text(attachments[index].displayName, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Muted)
                    Box(Modifier.weight(1f).fillMaxWidth().testTag("attachment-viewer")) {
                        Preview(attachments[index], maxDimension = 1600, contentScale = ContentScale.Fit)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(enabled = index > 0, onClick = { selectedId = attachments[index - 1].id }) { Text("上一张") }
                        OutlinedButton(enabled = index < attachments.lastIndex, onClick = { selectedId = attachments[index + 1].id }) { Text("下一张") }
                    }
                }
            }
        }
    }
}

private sealed interface PreviewState {
    data object Loading : PreviewState
    data class Loaded(val bitmap: Bitmap) : PreviewState
    data class Failed(val message: String) : PreviewState
}

@Composable
private fun Preview(attachment: AttachmentRef, maxDimension: Int, contentScale: ContentScale) {
    val context = LocalContext.current
    val store = remember(context.applicationContext) { AttachmentStore(context.applicationContext) }
    val state by produceState<PreviewState>(PreviewState.Loading, attachment.relativePath, maxDimension) {
        value = PreviewState.Loading
        value = try {
            PreviewState.Loaded(store.loadPreview(attachment, maxDimension))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            PreviewState.Failed(failure.message ?: "图片暂时无法读取。")
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val current = state) {
            PreviewState.Loading -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            is PreviewState.Loaded -> Image(
                bitmap = current.bitmap.asImageBitmap(),
                contentDescription = attachment.displayName,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
            is PreviewState.Failed -> Text(
                current.message,
                modifier = Modifier.padding(10.dp).testTag("attachment-error-${attachment.id}"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
