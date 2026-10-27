package cn.edu.hust.ddlsaver.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.Importance

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val deadlineAt: Long,
    val sprintMinutes: Long?,
    val description: String,
    val importance: Importance,
    val completedAt: Long?,
    val archivedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val revision: Long,
) {
    fun toDomain() = DdlEvent(
        id, title, deadlineAt, sprintMinutes, description, importance,
        completedAt, archivedAt, createdAt, updatedAt, revision,
    )
}

@Entity(tableName = "settings")
data class SettingsEntity(@PrimaryKey val id: Int = 1, val urgentMinutes: Long)

class SaverConverters {
    @TypeConverter
    fun importanceToString(value: Importance): String = value.name

    @TypeConverter
    fun stringToImportance(value: String): Importance = Importance.valueOf(value)
}
