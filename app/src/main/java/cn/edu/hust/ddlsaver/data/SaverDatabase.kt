package cn.edu.hust.ddlsaver.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [EventEntity::class, SettingsEntity::class, SourceEntity::class, TagEntity::class,
        EventTagEntity::class, CustomFieldEntity::class, AttachmentEntity::class],
    version = 2,
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

        fun create(context: Context): SaverDatabase = Room.databaseBuilder(
            context.applicationContext,
            SaverDatabase::class.java,
            "ddl-saver.db",
        ).addMigrations(MIGRATION_1_2).build()
    }
}
