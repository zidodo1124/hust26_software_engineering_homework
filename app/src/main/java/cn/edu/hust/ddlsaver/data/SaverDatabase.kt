package cn.edu.hust.ddlsaver.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.edu.hust.ddlsaver.reminders.ReminderPlanEntity
import cn.edu.hust.ddlsaver.reminders.ReminderControlEntity

@Database(
    entities = [EventEntity::class, SettingsEntity::class, SourceEntity::class, TagEntity::class,
        EventTagEntity::class, CustomFieldEntity::class, AttachmentEntity::class, ReminderPlanEntity::class, ReminderControlEntity::class,
        RecurrenceSeriesEntity::class, RecurrenceOccurrenceEntity::class],
    version = 5,
    exportSchema = true,
)
@TypeConverters(SaverConverters::class)
abstract class SaverDatabase : RoomDatabase() {
    abstract fun dao(): SaverDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN sourceId TEXT")
                db.execSQL("ALTER TABLE settings ADD COLUMN expiredPolicy TEXT NOT NULL DEFAULT 'KEEP'")
                db.execSQL("CREATE TABLE IF NOT EXISTS sources (id TEXT NOT NULL, name TEXT NOT NULL, isActive INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS tags (id TEXT NOT NULL, name TEXT NOT NULL, isActive INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS event_tags (eventId TEXT NOT NULL, tagId TEXT NOT NULL, PRIMARY KEY(eventId, tagId), FOREIGN KEY(eventId) REFERENCES events(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(tagId) REFERENCES tags(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_event_tags_tagId ON event_tags(tagId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS custom_fields (eventId TEXT NOT NULL, id TEXT NOT NULL, label TEXT NOT NULL, value TEXT NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(eventId, id), FOREIGN KEY(eventId) REFERENCES events(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE TABLE IF NOT EXISTS attachments (id TEXT NOT NULL, eventId TEXT NOT NULL, relativePath TEXT NOT NULL, mimeType TEXT NOT NULL, displayName TEXT NOT NULL, byteSize INTEGER NOT NULL, width INTEGER NOT NULL, height INTEGER NOT NULL, position INTEGER NOT NULL, PRIMARY KEY(id), FOREIGN KEY(eventId) REFERENCES events(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_eventId ON attachments(eventId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_attachments_relativePath ON attachments(relativePath)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN dailySummaryEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE settings ADD COLUMN dailySummaryMinuteOfDay INTEGER NOT NULL DEFAULT 480")
                db.execSQL("ALTER TABLE settings ADD COLUMN dangerReminderEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE settings ADD COLUMN deadlineReminderEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE settings ADD COLUMN deadlineLeadMinutes INTEGER NOT NULL DEFAULT 60")
                db.execSQL("CREATE TABLE IF NOT EXISTS reminder_plans (`key` TEXT NOT NULL, kind TEXT NOT NULL, eventId TEXT, deadlineAt INTEGER, eventRevision INTEGER, triggerAt INTEGER NOT NULL, day TEXT, notificationTag TEXT NOT NULL, active INTEGER NOT NULL, state TEXT NOT NULL, lastAttemptAt INTEGER, lastError TEXT, cancelPending INTEGER NOT NULL, PRIMARY KEY(`key`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS reminder_control (id INTEGER NOT NULL, alarmIdentity TEXT NOT NULL, dailyDate TEXT, dailyMinuteOfDay INTEGER, scheduledTriggerAt INTEGER, desiredTriggerAt INTEGER, scheduleMode TEXT, alarmDirty INTEGER NOT NULL, lastError TEXT, PRIMARY KEY(id))")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE settings ADD COLUMN characterStyle TEXT NOT NULL DEFAULT 'CLASSIC'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS recurrence_series (id TEXT NOT NULL, firstDeadline INTEGER NOT NULL, frequency TEXT NOT NULL, untilEpochDay INTEGER, occurrenceCount INTEGER, revision INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("CREATE TABLE IF NOT EXISTS recurrence_occurrences (seriesId TEXT NOT NULL, occurrenceIndex INTEGER NOT NULL, scheduledDeadline INTEGER NOT NULL, eventId TEXT, skipped INTEGER NOT NULL, PRIMARY KEY(seriesId, occurrenceIndex), FOREIGN KEY(seriesId) REFERENCES recurrence_series(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_recurrence_occurrences_eventId ON recurrence_occurrences(eventId)")
            }
        }

        fun create(context: Context): SaverDatabase = Room.databaseBuilder(
            context.applicationContext,
            SaverDatabase::class.java,
            "ddl-saver.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
    }
}
