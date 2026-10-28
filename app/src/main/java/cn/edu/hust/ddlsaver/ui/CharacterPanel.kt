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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    Surface(modifier.testTag("character-panel"), shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("蓝色大肥鱼", color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stacked = maxWidth < 270.dp || LocalDensity.current.fontScale > 1.3f
                val imageModifier = Modifier.width(if (style == CharacterStyle.CHIBI) 108.dp else 88.dp)
                    .height(if (style == CharacterStyle.CHIBI) 124.dp else 158.dp)
                if (stacked) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CharacterArtwork(style, feedback.state, imageModifier)
                        CharacterMessage(feedback, Modifier.fillMaxWidth())
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        CharacterArtwork(style, feedback.state, imageModifier)
                        CharacterMessage(feedback, Modifier.weight(1f))
                    }
                }
            }
            TextButton({ creditsOpen = true }, modifier = Modifier.align(Alignment.End).testTag("character-credits")) {
                Text("素材与署名")
            }
        }
    }
    if (creditsOpen) CharacterCreditsDialog(style = style) { creditsOpen = false }
}

@Composable
private fun CharacterMessage(feedback: CharacterFeedback, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(feedback.title, color = Ink, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("character-status"))
        Text(feedback.line, color = Muted, modifier = Modifier.testTag("character-line"))
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
            Text(if (style == CharacterStyle.CLASSIC) "完整立绘与细腻表情，保留熟悉的陪伴。" else "圆润的 Q 版形象，用不同表情陪你推进事项。",
                color = Muted, style = MaterialTheme.typography.bodyMedium)
            Text("保存设置后生效；两种形象使用相同的状态提醒和台词。", color = Muted,
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
