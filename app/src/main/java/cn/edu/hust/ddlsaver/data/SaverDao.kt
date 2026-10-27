package cn.edu.hust.ddlsaver.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import cn.edu.hust.ddlsaver.reminders.ReminderPlanEntity
import cn.edu.hust.ddlsaver.reminders.ReminderControlEntity

@Dao
interface SaverDao {
    @Transaction
    @Query("SELECT * FROM events ORDER BY deadlineAt ASC, createdAt ASC, id ASC")
    fun observeEvents(): Flow<List<EventWithRelations>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun findEvent(id: String): EventEntity?

    @Transaction
    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun findEventWithRelations(id: String): EventWithRelations?

    @Query("SELECT * FROM events WHERE id IN (:ids)")
    suspend fun findEvents(ids: Set<String>): List<EventEntity>

    @Query("SELECT * FROM events WHERE deadlineAt <= :now AND archivedAt IS NULL AND (:includeCompleted OR completedAt IS NULL)")
    suspend fun findExpired(now: Long, includeCompleted: Boolean): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvent(event: EventEntity)

    @Update
    suspend fun updateEvent(event: EventEntity): Int

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEvent(id: String): Int

    @Query("SELECT * FROM settings WHERE id = 1")
    fun observeSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun readSettings(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SettingsEntity)

    @Query("SELECT * FROM sources ORDER BY name, id")
    fun observeSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM tags ORDER BY name, id")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM sources")
    suspend fun readSources(): List<SourceEntity>

    @Query("SELECT * FROM tags")
    suspend fun readTags(): List<TagEntity>

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun findSource(id: String): SourceEntity?

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun findTag(id: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSource(source: SourceEntity)

    @Update
    suspend fun updateSource(source: SourceEntity): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: TagEntity)

    @Update
    suspend fun updateTag(tag: TagEntity): Int

    @Query("SELECT tagId FROM event_tags WHERE eventId = :eventId")
    suspend fun eventTagIds(eventId: String): List<String>

    @Query("DELETE FROM event_tags WHERE eventId = :eventId")
    suspend fun clearEventTags(eventId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEventTags(tags: List<EventTagEntity>)

    @Query("DELETE FROM custom_fields WHERE eventId = :eventId")
    suspend fun clearCustomFields(eventId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustomFields(fields: List<CustomFieldEntity>)

    @Query("SELECT * FROM attachments WHERE id = :id OR relativePath = :relativePath")
    suspend fun findAttachmentOwners(id: String, relativePath: String): List<AttachmentEntity>

    @Query("DELETE FROM attachments WHERE eventId = :eventId")
    suspend fun clearAttachments(eventId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Query("SELECT relativePath FROM attachments")
    suspend fun referencedAttachmentPaths(): List<String>

    @Query("SELECT * FROM events ORDER BY deadlineAt, id")
    suspend fun readAllEventEntities(): List<EventEntity>

    @Query("SELECT * FROM reminder_plans ORDER BY triggerAt, `key`")
    suspend fun readReminderPlans(): List<ReminderPlanEntity>

    @Query("SELECT * FROM reminder_plans WHERE `key` = :key")
    suspend fun readReminderPlan(key: String): ReminderPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putReminderPlan(plan: ReminderPlanEntity)

    @Query("SELECT * FROM reminder_control WHERE id = 1")
    suspend fun readReminderControl(): ReminderControlEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putReminderControl(control: ReminderControlEntity)
}
