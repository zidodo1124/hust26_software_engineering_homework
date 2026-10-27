package cn.edu.hust.ddlsaver.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.edu.hust.ddlsaver.domain.CharacterFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Offline artwork; failure to decode never hides the textual status. */
@Composable
fun CharacterPanel(feedback: CharacterFeedback, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, context, feedback.imageAsset) {
        value = null
        value = withContext(Dispatchers.IO) {
            try {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.assets.open(feedback.imageAsset).use { BitmapFactory.decodeStream(it, null, options) }
                var sample = 1
                while (maxOf(options.outWidth, options.outHeight) / (sample * 2) >= 768) sample *= 2
                context.assets.open(feedback.imageAsset).use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                }
            } catch (_: IOException) { null }
            catch (_: IllegalArgumentException) { null }
        }
    }
    var creditsOpen by rememberSaveable { mutableStateOf(false) }
    Surface(modifier.testTag("character-panel"), shape = RoundedCornerShape(20.dp), color = Color.White) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (bitmap != null) {
                    Image(bitmap!!.asImageBitmap(), feedback.imageDescription,
                        Modifier.width(96.dp).height(174.dp).testTag("character-image"), contentScale = ContentScale.Fit)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("鲸鱼娘 · ${feedback.title}", color = Ink, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("character-status"))
                    Text(feedback.line, color = Muted, modifier = Modifier.testTag("character-line"))
                }
            }
            TextButton({ creditsOpen = true }, modifier = Modifier.align(Alignment.End).testTag("character-credits")) {
                Text("素材与署名")
            }
        }
    }
    if (creditsOpen) CharacterCreditsDialog { creditsOpen = false }
}

@Composable
fun CharacterCreditsDialog(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("角色素材与署名") },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("本应用使用非官方鲸鱼娘插画，与 DeepSeek 及素材作者不存在官方合作或背书关系。插画保留了上游的鲸鱼图形，未用作本应用图标或品牌。")
                Text("创作链：上善（原角色） → ZipZipPipe（女仆二次设计） → Small-tailqwq / dsh-deep-whale（上游立绘） → JAdpp / dsh-whale-galgame（新增表情）。")
                Text("四张图片按 CC BY-NC-SA 4.0 使用：保留署名、限非商业使用，改编素材按相同方式共享。本项目原字节复制图片，未修改画面或元数据；预设台词为本项目编写。")
                Text("来源：https://github.com/JAdpp/dsh-whale-galgame\n素材版本：046b848f244450e6e636335766276bf3343fe84b\n许可：https://creativecommons.org/licenses/by-nc-sa/4.0/")
                Text("完整素材来源与许可已随应用保留，也可在项目仓库的角色素材说明中查看。")
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("关闭") } })
}
