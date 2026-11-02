package cn.edu.hust.ddlsaver.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverter
import cn.edu.hust.ddlsaver.domain.AppSettings
import cn.edu.hust.ddlsaver.domain.AttachmentRef
import cn.edu.hust.ddlsaver.domain.Category
import cn.edu.hust.ddlsaver.domain.CharacterStyle
import cn.edu.hust.ddlsaver.domain.CustomField
import cn.edu.hust.ddlsaver.domain.DdlEvent
import cn.edu.hust.ddlsaver.domain.ExpiredPolicy
import cn.edu.hust.ddlsaver.domain.Importance
import cn.edu.hust.ddlsaver.domain.RecurrenceRef

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
    val sourceId: String? = null,
) {
    fun toDomain() = DdlEvent(
        id, title, deadlineAt, sprintMinutes, description, importance,
        completedAt, archivedAt, createdAt, updatedAt, revision,
    )
}

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val urgentMinutes: Long,
    @ColumnInfo(defaultValue = "'KEEP'") val expiredPolicy: ExpiredPolicy = ExpiredPolicy.KEEP,
    @ColumnInfo(defaultValue = "1") val dailySummaryEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "480") val dailySummaryMinuteOfDay: Int = 480,
    @ColumnInfo(defaultValue = "1") val dangerReminderEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "1") val deadlineReminderEnabled: Boolean = true,
    @ColumnInfo(defaultValue = "60") val deadlineLeadMinutes: Long = 60,
    @ColumnInfo(defaultValue = "'CLASSIC'") val characterStyle: CharacterStyle = CharacterStyle.CLASSIC,
) {
    fun toDomain() = AppSettings(urgentMinutes, expiredPolicy, dailySummaryEnabled,
        dailySummaryMinuteOfDay, dangerReminderEnabled, deadlineReminderEnabled, deadlineLeadMinutes, characterStyle)
}

internal fun AppSettings.toEntity() = SettingsEntity(urgentMinutes = urgentMinutes,
    expiredPolicy = expiredPolicy, dailySummaryEnabled = dailySummaryEnabled,
    dailySummaryMinuteOfDay = dailySummaryMinuteOfDay, dangerReminderEnabled = dangerReminderEnabled,
    deadlineReminderEnabled = deadlineReminderEnabled, deadlineLeadMinutes = deadlineLeadMinutes,
    characterStyle = characterStyle)

@Entity(tableName = "sources")
data class SourceEntity(@PrimaryKey val id: String, val name: String, val isActive: Boolean = true) {
    fun toDomain() = Category(id, name, isActive)
}

@Entity(tableName = "tags")
data class TagEntity(@PrimaryKey val id: String, val name: String, val isActive: Boolean = true) {
    fun toDomain() = Category(id, name, isActive)
}

@Entity(
    tableName = "event_tags",
    primaryKeys = ["eventId", "tagId"],
    foreignKeys = [
        ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TagEntity::class, parentColumns = ["id"], childColumns = ["tagId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("tagId")],
)
data class EventTagEntity(val eventId: String, val tagId: String)

@Entity(
    tableName = "custom_fields",
    primaryKeys = ["eventId", "id"],
    foreignKeys = [ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE)],
)
data class CustomFieldEntity(val eventId: String, val id: String, val label: String, val value: String, val position: Int) {
    fun toDomain() = CustomField(id, label, value)
}

@Entity(
    tableName = "attachments",
    foreignKeys = [ForeignKey(entity = EventEntity::class, parentColumns = ["id"], childColumns = ["eventId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("eventId"), Index(value = ["relativePath"], unique = true)],
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val relativePath: String,
    val mimeType: String,
    val displayName: String,
    val byteSize: Long,
    val width: Int,
    val height: Int,
    val position: Int,
) {
    fun toDomain() = AttachmentRef(id, relativePath, mimeType, displayName, byteSize, width, height)
}

data class EventWithRelations(
    @Embedded val event: EventEntity,
    @Relation(parentColumn = "sourceId", entityColumn = "id") val source: SourceEntity?,
    @Relation(parentColumn = "id", entityColumn = "id", associateBy = Junction(EventTagEntity::class, parentColumn = "eventId", entityColumn = "tagId"))
    val tags: List<TagEntity>,
    @Relation(parentColumn = "id", entityColumn = "eventId") val fields: List<CustomFieldEntity>,
    @Relation(parentColumn = "id", entityColumn = "eventId") val attachments: List<AttachmentEntity>,
    @Relation(parentColumn = "id", entityColumn = "eventId") val recurrence: RecurrenceOccurrenceEntity?,
) {
    fun toDomain() = event.toDomain().copy(
        source = source?.toDomain(),
        tags = tags.sortedWith(compareBy<TagEntity> { it.name }.thenBy { it.id }).map { it.toDomain() },
        customFields = fields.sortedBy { it.position }.map { it.toDomain() },
        attachments = attachments.sortedBy { it.position }.map { it.toDomain() },
        recurrence = recurrence?.let { RecurrenceRef(it.seriesId, it.occurrenceIndex) },
    )
}

class SaverConverters {
    @TypeConverter
    fun importanceToString(value: Importance): String = value.name

    @TypeConverter
    fun stringToImportance(value: String): Importance = Importance.valueOf(value)

    @TypeConverter
    fun expiredPolicyToString(value: ExpiredPolicy): String = value.name

    @TypeConverter
    fun stringToExpiredPolicy(value: String): ExpiredPolicy = ExpiredPolicy.valueOf(value)

    @TypeConverter
    fun characterStyleToString(value: CharacterStyle): String = value.name

    @TypeConverter
    fun stringToCharacterStyle(value: String): CharacterStyle = CharacterStyle.fromStored(value)
}
