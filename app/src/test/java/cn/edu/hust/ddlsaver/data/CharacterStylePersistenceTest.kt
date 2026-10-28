package cn.edu.hust.ddlsaver.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cn.edu.hust.ddlsaver.domain.AppSettings
import cn.edu.hust.ddlsaver.domain.CharacterStyle
import cn.edu.hust.ddlsaver.domain.ExpiredPolicy
import cn.edu.hust.ddlsaver.reminders.ReminderControlEntity
import cn.edu.hust.ddlsaver.reminders.ReminderPlanEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CharacterStylePersistenceTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test fun upgradeFromV3PreservesSettingsEventAndDeliveryLedgerWithClassicDefault() = runBlocking {
        val name = "style-v3.db"
        createV3(name) { legacy ->
            legacy.execSQL("INSERT INTO settings VALUES (1, 321, 'AUTO_ARCHIVE', 0, 547, 1, 0, 45)")
            legacy.execSQL("INSERT INTO events VALUES ('kept', '原事项', 1893567600000, 30, '说明', 'HIGH', NULL, NULL, 10, 20, 7, NULL)")
            legacy.execSQL("INSERT INTO reminder_plans VALUES ('saved-key', 'DANGER', 'kept', 1893567600000, 7, 1893565800000, NULL, 'saved-tag', 1, 'SENT', 1893565800100, NULL, 0)")
            legacy.execSQL("INSERT INTO reminder_control VALUES (1, 'ddl-saver://reminders/wake', NULL, NULL, 1893566000000, 1893566000000, 'exact', 0, NULL)")
        }
        var database: SaverDatabase? = null
        try {
            database = open(name)
            val repository = RoomEventRepository(database)
            assertEquals(AppSettings(321, ExpiredPolicy.AUTO_ARCHIVE, false, 547, true, false, 45), repository.settings.first())
            val event = repository.events.first().single()
            assertEquals("原事项", event.title)
            assertEquals(7L, event.revision)
            assertEquals("说明", event.description)
            assertEquals(listOf(ReminderPlanEntity("saved-key", "DANGER", "kept", 1893567600000,
                7, 1893565800000, null, "saved-tag", state = "SENT", lastAttemptAt = 1893565800100)),
                database.dao().readReminderPlans())
            assertEquals(ReminderControlEntity(scheduledTriggerAt = 1893566000000, desiredTriggerAt = 1893566000000,
                scheduleMode = "exact", alarmDirty = false), database.dao().readReminderControl())
            assertEquals(4, database.openHelper.readableDatabase.version)
        } finally { database?.close(); context.deleteDatabase(name) }
    }

    @Test fun emptyV3SettingsRemainAbsentAndUseClassicDefaults() = runBlocking {
        val name = "style-empty.db"
        createV3(name) { }
        val database = open(name)
        try {
            assertEquals(AppSettings(), RoomEventRepository(database).settings.first())
            assertNull(database.dao().readSettings())
        } finally { database.close(); context.deleteDatabase(name) }
    }

    @Test fun bothStylesPersistAcrossDatabaseReopenAndUnknownValueFallsBack() = runBlocking {
        val name = "style-save.db"
        var database = open(name)
        try {
            for (style in listOf(CharacterStyle.CHIBI, CharacterStyle.CLASSIC)) {
                val settings = AppSettings(urgentMinutes = 345, dailySummaryEnabled = false,
                    deadlineLeadMinutes = 17, characterStyle = style)
                RoomEventRepository(database).saveSettings(settings)
                database.close()
                database = open(name)
                assertEquals(settings, RoomEventRepository(database).settings.first())
            }
            database.openHelper.writableDatabase.execSQL("UPDATE settings SET characterStyle = 'FUTURE_STYLE' WHERE id = 1")
            database.close()
            database = open(name)
            val recovered = RoomEventRepository(database).settings.first()
            assertEquals(CharacterStyle.CLASSIC, recovered.characterStyle)
            assertEquals(345L, recovered.urgentMinutes)
            assertEquals(17L, recovered.deadlineLeadMinutes)
        } finally { database.close(); context.deleteDatabase(name) }
    }

    private fun open(name: String) = Room.databaseBuilder(context, SaverDatabase::class.java, name)
        .addMigrations(SaverDatabase.MIGRATION_3_4).build()

    private fun createV3(name: String, populate: (SQLiteDatabase) -> Unit) {
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        checkNotNull(path.parentFile).mkdirs()
        val relative = "schemas/cn.edu.hust.ddlsaver.data.SaverDatabase/3.json"
        val fixture = sequenceOf(File(relative), File("app", relative)).first { it.isFile }
        val schema = JSONObject(fixture.readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(path, null).use { legacy ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                legacy.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices")
                if (indices != null) for (j in 0 until indices.length()) legacy.execSQL(indices.getJSONObject(j).getString("createSql")
                    .replace("\${TABLE_NAME}", table))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) legacy.execSQL(setup.getString(i))
            populate(legacy)
            legacy.version = 3
        }
    }
}
