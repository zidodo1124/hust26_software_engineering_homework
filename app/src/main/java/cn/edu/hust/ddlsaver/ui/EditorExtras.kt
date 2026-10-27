package cn.edu.hust.ddlsaver.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.edu.hust.ddlsaver.domain.AttachmentRef
import cn.edu.hust.ddlsaver.domain.Category
import cn.edu.hust.ddlsaver.domain.CustomField
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A Bundle-safe representation keeps the complete draft through Activity recreation. */
internal object DraftJson {
    fun fields(items: List<CustomField>): String = JSONArray().apply {
        items.forEach { put(JSONObject().put("id", it.id).put("label", it.label).put("value", it.value)) }
    }.toString()
    fun fields(value: String): List<CustomField> = JSONArray(value).let { array ->
        (0 until array.length()).map { array.getJSONObject(it).let { o ->
            CustomField(o.getString("id"), o.getString("label"), o.getString("value"))
        } }
    }
    fun attachments(items: List<AttachmentRef>): String = JSONArray().apply {
        items.forEach { put(JSONObject().put("id", it.id).put("path", it.relativePath)
            .put("mime", it.mimeType).put("name", it.displayName).put("size", it.byteSize)
            .put("width", it.width).put("height", it.height)) }
    }.toString()
    fun attachments(value: String): List<AttachmentRef> = JSONArray(value).let { array ->
        (0 until array.length()).map { array.getJSONObject(it).let { o ->
            AttachmentRef(o.getString("id"), o.getString("path"), o.getString("mime"),
                o.getString("name"), o.getLong("size"), o.getInt("width"), o.getInt("height"))
        } }
    }
    fun ids(items: Collection<String>): String = JSONArray(items.toList()).toString()
    fun ids(value: String): Set<String> = JSONArray(value).let { array ->
        (0 until array.length()).map { array.getString(it) }.toSet()
    }
}

@Composable
internal fun EventCategories(
    sources: List<Category>, tags: List<Category>, sourceId: String?, tagIds: Set<String>,
    originalSource: String?, originalTags: Set<String>, busy: Boolean,
    onSource: (String?) -> Unit, onTags: (Set<String>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("来源", fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("editor-source-options"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(sourceId == null, { onSource(null) }, label = { Text("未指定") }, enabled = !busy)
            sources.filter { it.isActive || it.id == originalSource }.forEach { item ->
                FilterChip(sourceId == item.id, { onSource(item.id) }, enabled = !busy,
                    modifier = Modifier.testTag("editor-source-${item.id}"),
                    label = { Text(item.name + if (item.isActive) "" else "（已停用）") })
            }
        }
        Text("标签（可多选）", fontWeight = FontWeight.SemiBold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("editor-tag-options"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tags.filter { it.isActive || it.id in originalTags }.forEach { item ->
                FilterChip(item.id in tagIds, { onTags(if (item.id in tagIds) tagIds - item.id else tagIds + item.id) },
                    enabled = !busy, modifier = Modifier.testTag("editor-tag-${item.id}"),
                    label = { Text(item.name + if (item.isActive) "" else "（已停用）") })
            }
        }
        Text("来源与标签可在设置中的“分类管理”维护。停用后仍保留已有事项的关联。", color = Muted,
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun CustomFieldEditor(fields: List<CustomField>, busy: Boolean, onChange: (List<CustomField>) -> Unit) {
    val clipboard = LocalClipboardManager.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("自定义字段", fontWeight = FontWeight.SemiBold)
        Text("为当前事项补充地点、联系人或网址等文本信息。", color = Muted, style = MaterialTheme.typography.bodySmall)
        fields.forEachIndexed { index, field -> key(field.id) {
            OutlinedTextField(field.label, { value -> onChange(fields.map { if (it.id == field.id) it.copy(label = value) else it }) },
                enabled = !busy, label = { Text("字段名称 ${index + 1}") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("field-label-$index"))
            OutlinedTextField(field.value, { value -> onChange(fields.map { if (it.id == field.id) it.copy(value = value) else it }) },
                enabled = !busy, label = { Text("字段内容 ${index + 1}") }, maxLines = 6,
                modifier = Modifier.fillMaxWidth().testTag("field-value-$index"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton({ clipboard.setText(AnnotatedString(field.value)) }, enabled = field.value.isNotEmpty()) { Text("复制内容") }
                TextButton({ onChange(fields.filterNot { it.id == field.id }) }, enabled = !busy,
                    modifier = Modifier.testTag("field-remove-$index")) { Text("移除字段") }
            }
        } }
        OutlinedButton({ onChange(fields + CustomField(UUID.randomUUID().toString(), "", "")) }, enabled = !busy,
            modifier = Modifier.testTag("field-add")) { Text("添加自定义字段") }
    }
}
