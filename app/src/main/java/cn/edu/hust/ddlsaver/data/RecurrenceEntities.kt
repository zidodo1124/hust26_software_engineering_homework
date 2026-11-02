package cn.edu.hust.ddlsaver.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Series data is separate from events; old single events need no conversion. */
@Entity(tableName = "recurrence_series")
data class RecurrenceSeriesEntity(
    @PrimaryKey val id: String,
    val firstDeadline: Long,
    val frequency: String,
    val untilEpochDay: Long?,
    val occurrenceCount: Int?,
    val revision: Long,
    val templateJson: String = "{}",
    val monthlyAnchorDay: Int? = null,
)

/** A unique series/index pair prevents a period being persisted twice. */
@Entity(
    tableName = "recurrence_occurrences",
    primaryKeys = ["seriesId", "occurrenceIndex"],
    foreignKeys = [ForeignKey(entity = RecurrenceSeriesEntity::class, parentColumns = ["id"],
        childColumns = ["seriesId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["eventId"], unique = true)],
)
data class RecurrenceOccurrenceEntity(
    val seriesId: String,
    val occurrenceIndex: Long,
    val scheduledDeadline: Long,
    val eventId: String?,
    val skipped: Boolean,
)
