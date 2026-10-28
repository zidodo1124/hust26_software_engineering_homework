package cn.edu.hust.ddlsaver.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.res.AssetManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import cn.edu.hust.ddlsaver.domain.CharacterFeedback
import cn.edu.hust.ddlsaver.domain.CharacterState
import cn.edu.hust.ddlsaver.domain.CharacterStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

internal data class CharacterArtworkSpec(val asset: String, val column: Int? = null, val row: Int? = null)

internal fun characterArtwork(style: CharacterStyle, state: CharacterState): CharacterArtworkSpec =
    if (style == CharacterStyle.CHIBI) {
        val (column, row) = when (state) {
            CharacterState.NORMAL -> 6 to 0
            CharacterState.COMPLETED -> 1 to 4
            CharacterState.DANGER -> 1 to 7
            CharacterState.SOON, CharacterState.OVERDUE -> 1 to 8
        }
        CharacterArtworkSpec("character/chibi/spritesheet.webp", column, row)
    } else CharacterArtworkSpec("character/" + when (state) {
        CharacterState.NORMAL -> "maid-left.webp"
        CharacterState.COMPLETED -> "whale-cheerful.webp"
        CharacterState.DANGER -> "whale-angry.webp"
        CharacterState.SOON, CharacterState.OVERDUE -> "whale-serious.webp"
    })

private val ChibiDecodeLock = Any()

/** Original files stay intact. Serial decoding bounds the atlas's temporary pixel allocation. */
internal fun decodeCharacterArtwork(assets: AssetManager, spec: CharacterArtworkSpec): Bitmap? = try {
    if (spec.column != null && spec.row != null) {
        synchronized(ChibiDecodeLock) {
            require(spec.column in 0 until 8 && spec.row in 0 until 11)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            assets.open(spec.asset).use { BitmapFactory.decodeStream(it, null, bounds) }
            require(bounds.outWidth == 1536 && bounds.outHeight == 2288)
            // This fixed atlas needs about 13.4 MiB while decoding. Only the 192 × 208 frame is retained.
            val sheet = assets.open(spec.asset).use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 })
            } ?: return@synchronized null
            try {
                Bitmap.createBitmap(sheet, spec.column * 192, spec.row * 208, 192, 208)
            } finally { sheet.recycle() }
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        assets.open(spec.asset).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 768) sample *= 2
        assets.open(spec.asset).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }
} catch (_: IOException) { null }
catch (_: IllegalArgumentException) { null }

@Composable
internal fun CharacterArtwork(style: CharacterStyle, state: CharacterState, modifier: Modifier = Modifier,
    tag: String = "character-image") {
    val context = LocalContext.current
    val spec = characterArtwork(style, state)
    // A keyed subtree clears the old bitmap immediately when a style or expression changes.
    key(spec) {
        val bitmap by produceState<Bitmap?>(null, context, spec) {
            value = withContext(Dispatchers.IO) { decodeCharacterArtwork(context.assets, spec) }
        }
        bitmap?.let { image ->
            Image(image.asImageBitmap(), "蓝色大肥鱼 · ${style.label} · " + when (state) {
                CharacterState.NORMAL -> "普通表情"
                CharacterState.COMPLETED -> "快乐表情"
                CharacterState.DANGER -> "着急催促的表情"
                CharacterState.SOON, CharacterState.OVERDUE -> "认真提醒的表情"
            }, modifier.testTag(tag), contentScale = ContentScale.Fit)
        }
    }
}

/** Offline artwork; failure to decode never hides the textual status. */
@Composable
fun CharacterPanel(feedback: CharacterFeedback, modifier: Modifier = Modifier, style: CharacterStyle = CharacterStyle.CLASSIC) {
    var creditsOpen by rememberSaveable { mutableStateOf(false) }
    CharacterPanelLayout(feedback, modifier, style, onCredits = { creditsOpen = true }) { imageModifier ->
        CharacterArtwork(style, feedback.state, imageModifier)
    }
    if (creditsOpen) CharacterCreditsDialog(style = style) { creditsOpen = false }
}

/** The artwork slot may be empty while loading or after a decode failure; text stays available. */
@Composable
internal fun CharacterPanelLayout(
    feedback: CharacterFeedback,
    modifier: Modifier = Modifier,
    style: CharacterStyle = CharacterStyle.CLASSIC,
    onCredits: () -> Unit,
    artwork: @Composable (Modifier) -> Unit,
) {
    Surface(modifier.testTag("character-panel"), shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("蓝色大肥鱼", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(feedback.title, color = Ink, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("character-status"))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stacked = maxWidth < 270.dp || LocalDensity.current.fontScale > 1.3f
                val imageModifier = Modifier.width(if (style == CharacterStyle.CHIBI) 108.dp else 88.dp)
                    .height(if (style == CharacterStyle.CHIBI) 124.dp else 158.dp)
                if (stacked) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        artwork(imageModifier)
                        CharacterMessage(feedback.line, Modifier.fillMaxWidth(), tailOnTop = true)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        artwork(imageModifier)
                        CharacterMessage(feedback.line, Modifier.weight(1f), tailOnTop = false)
                    }
                }
            }
            TextButton(onCredits, modifier = Modifier.align(Alignment.End).testTag("character-credits")) {
                Text("素材与署名")
            }
        }
    }
}

@Composable
private fun CharacterMessage(line: String, modifier: Modifier, tailOnTop: Boolean) {
    val bubbleColor = MaterialTheme.colorScheme.primaryContainer
    Box(modifier.testTag("character-bubble")) {
        // Reserve the tail inside the bubble's measured bounds so it cannot overlap its neighbour.
        val tailModifier = if (tailOnTop) {
            Modifier.align(Alignment.TopCenter).size(width = 18.dp, height = 9.dp)
        } else {
            Modifier.align(Alignment.CenterStart).size(width = 9.dp, height = 18.dp)
        }
        Spacer(tailModifier.testTag(if (tailOnTop) "character-bubble-tail-top" else "character-bubble-tail-start")
            .drawWithCache {
                val path = Path().apply {
                    if (tailOnTop) {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                    } else if (layoutDirection == LayoutDirection.Ltr) {
                        moveTo(0f, size.height / 2f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, size.height)
                    } else {
                        moveTo(size.width, size.height / 2f)
                        lineTo(0f, size.height)
                        lineTo(0f, 0f)
                    }
                    close()
                }
                onDrawBehind { drawPath(path, bubbleColor) }
            })
        Surface(
            modifier = Modifier.padding(start = if (tailOnTop) 0.dp else 8.dp, top = if (tailOnTop) 8.dp else 0.dp),
            color = bubbleColor,
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(line, color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp).testTag("character-line"))
        }
    }
}

@Composable
internal fun CharacterAppearancePicker(style: CharacterStyle, enabled: Boolean, onSelect: (CharacterStyle) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("角色形象", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("蓝色大肥鱼", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Column(Modifier.selectableGroup()) {
                CharacterStyle.entries.forEach { option ->
                    FilterChip(style == option, { onSelect(option) }, enabled = enabled,
                        label = { Text(option.label) }, modifier = Modifier.testTag("character-style-${option.name}"))
                }
            }
            CharacterArtwork(style, CharacterState.NORMAL,
                Modifier.height(150.dp).fillMaxWidth(), tag = "character-preview")
            Text(if (style == CharacterStyle.CLASSIC) "经典完整立绘，保留原有表情。" else "小巧圆润的 Q 版，表情随事项状态变化。",
                color = Muted, style = MaterialTheme.typography.bodyMedium)
            Text("点击“保存设置”后生效。切换形象不会改变事项或通知设置。", color = Muted,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun CharacterCreditsDialog(style: CharacterStyle = CharacterStyle.CLASSIC, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("角色素材与署名") },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("本应用使用非官方鲸鱼娘插画，与 DeepSeek 及素材作者不存在官方合作或背书关系。插画保留了上游的鲸鱼图形，未用作本应用图标或品牌。")
                if (style == CharacterStyle.CLASSIC) {
                    Text("经典立绘创作链：上善（原角色） → ZipZipPipe（女仆二次设计） → Small-tailqwq / dsh-deep-whale（上游立绘） → JAdpp / dsh-whale-galgame（新增表情）。")
                    Text("四张经典立绘按 CC BY-NC-SA 4.0 使用：保留署名、限非商业使用，改编素材按相同方式共享。本项目原字节复制图片，未修改画面或元数据。")
                    Text("来源：https://github.com/JAdpp/dsh-whale-galgame\n素材版本：046b848f244450e6e636335766276bf3343fe84b\n许可：https://creativecommons.org/licenses/by-nc-sa/4.0/")
                } else {
                    Text("Q 版图集：f0909172434 / dsh-deepseek-girl-pet。依据发布者声明，图集贡献按 MIT 许可发布；完整许可与版权声明已随应用保留。")
                    Text("原角色署名链：上善（原角色） → ZipZipPipe（女仆二次设计）。原角色内容依据上游 CC BY-NC-SA 4.0 声明保留署名及非商业条件，不能将底层角色权利统称为 MIT。")
                    Text("图集来源：https://github.com/f0909172434/dsh-deepseek-girl-pet\n素材版本：3e7df1d76014e4f43ba9ed3c9de53c2a62ba0d14\n本项目原字节保存图集，运行时仅显示所选表情格，不修改源文件。")
                }
                Text("预设台词为本项目编写。两种形象均仅用于本应用的非商业课程展示与事项反馈。")
                Text("完整素材来源与许可已随应用保留，也可在项目仓库的角色素材说明中查看。")
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("关闭") } })
}
