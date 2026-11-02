package cn.edu.hust.ddlsaver.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.edu.hust.ddlsaver.domain.Category

@Composable
internal fun CategoryManager(
    sources: List<Category>, tags: List<Category>, busy: Boolean, operationError: String?, successToken: Long,
    onBack: () -> Unit, onSave: (Boolean, String?, String) -> Unit, onToggle: (Boolean, Category) -> Unit,
    onClearError: () -> Unit,
) {
    var isSource by rememberSaveable { mutableStateOf(true) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var seenSuccess by rememberSaveable { mutableLongStateOf(successToken) }
    LaunchedEffect(successToken) {
        if (successToken != seenSuccess) { seenSuccess = successToken; editing = false; name = "" }
    }
    val back: () -> Unit = { if (!busy) { if (editing) editing = false else onBack() } }
    BackHandler(onBack = back)
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("分类管理", back, enabled = !busy)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("来源可以填课程、社团，标签用来细分。停用后，已有事项里的分类信息仍会保留。", color = Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(isSource, { isSource = true }, enabled = !busy && !editing, label = { Text("来源管理") }, modifier = Modifier.testTag("catalog-sources"))
                FilterChip(!isSource, { isSource = false }, enabled = !busy && !editing, label = { Text("标签管理") }, modifier = Modifier.testTag("catalog-tags"))
            }
            Button({ editingId = null; name = ""; submitted = false; onClearError(); editing = true },
                enabled = !busy && !editing, modifier = Modifier.testTag("catalog-add")) { Text(if (isSource) "新增来源" else "新增标签") }
            if (editing) Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (editingId == null) "新增${if (isSource) "来源" else "标签"}" else "修改分类名称",
                        style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(name, { name = it }, enabled = !busy, singleLine = true, label = { Text("名称") },
                        isError = submitted && name.isBlank(), modifier = Modifier.fillMaxWidth().testTag("catalog-name"))
                    if (submitted && name.isBlank()) Text("名称还没填。", color = MaterialTheme.colorScheme.error)
                    operationError?.let { ErrorNotice(it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button({ submitted = true; if (name.isNotBlank()) onSave(isSource, editingId, name) },
                            enabled = !busy, modifier = Modifier.testTag("catalog-save")) { Text("保存分类") }
                        TextButton({ editing = false; onClearError() }, enabled = !busy) { Text("取消") }
                    }
                }
            }
            val entries = if (isSource) sources else tags
            if (entries.isEmpty()) Text(if (isSource) "还没有来源，点“新增来源”添加一个。" else "还没有标签，点“新增标签”添加一个。", color = Muted)
            entries.forEach { category ->
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(if (category.isActive) "使用中" else "已停用", color = Muted)
                        }
                        Row {
                            TextButton({ editingId = category.id; name = category.name; submitted = false; onClearError(); editing = true },
                                enabled = !busy && !editing, modifier = Modifier.testTag("catalog-edit-${category.id}")) { Text("重命名") }
                            TextButton({ onToggle(isSource, category) }, enabled = !busy && !editing,
                                modifier = Modifier.testTag("catalog-toggle-${category.id}")) { Text(if (category.isActive) "停用" else "重新启用") }
                        }
                    }
                }
            }
            if (!editing) operationError?.let { ErrorNotice(it) }
        }
    }
}
