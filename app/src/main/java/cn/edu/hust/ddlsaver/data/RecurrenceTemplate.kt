package cn.edu.hust.ddlsaver.data

import cn.edu.hust.ddlsaver.domain.CustomField
import cn.edu.hust.ddlsaver.domain.EventDraft
import cn.edu.hust.ddlsaver.domain.EventValidationException
import cn.edu.hust.ddlsaver.domain.Importance
import org.json.JSONArray
import org.json.JSONObject

/** Versioned snapshot; images need a separate ownership design before they can be supported. */
internal object RecurrenceTemplate {
    fun encode(draft: EventDraft): String {
        if (draft.id != null || draft.attachments.isNotEmpty()) {
            throw EventValidationException("重复事项暂不支持从已有事项创建或携带图片附件。")
        }
        val fields = JSONArray()
        draft.customFields.forEach { fields.put(JSONObject().put("id", it.id).put("label", it.label).put("value", it.value)) }
        return JSONObject().put("version", 1).put("title", draft.title)
            .put("sprint", draft.sprintMinutes ?: JSONObject.NULL).put("description", draft.description)
            .put("importance", draft.importance.name).put("source", draft.sourceId ?: JSONObject.NULL)
            .put("tags", JSONArray(draft.tagIds.sorted())).put("fields", fields).toString()
    }

    fun decode(json: String, deadline: Long): EventDraft {
        val obj = JSONObject(json)
        require(obj.getInt("version") == 1) { "Unsupported recurrence template version" }
        val tags = obj.getJSONArray("tags")
        val fields = obj.getJSONArray("fields")
        return EventDraft(title = obj.getString("title"), deadlineAt = deadline,
            sprintMinutes = if (obj.isNull("sprint")) null else obj.getLong("sprint"),
            description = obj.getString("description"), importance = Importance.valueOf(obj.getString("importance")),
            sourceId = if (obj.isNull("source")) null else obj.getString("source"),
            tagIds = (0 until tags.length()).map { tags.getString(it) }.toSet(),
            customFields = (0 until fields.length()).map { index ->
                val field = fields.getJSONObject(index)
                CustomField(field.getString("id"), field.getString("label"), field.getString("value"))
            })
    }
}
