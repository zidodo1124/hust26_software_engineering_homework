package cn.edu.hust.ddlsaver.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import cn.edu.hust.ddlsaver.domain.AppSettings
import cn.edu.hust.ddlsaver.domain.AttachmentRef
import cn.edu.hust.ddlsaver.domain.CustomField
import cn.edu.hust.ddlsaver.domain.EventDraft
import cn.edu.hust.ddlsaver.domain.ExpiredPolicy
import cn.edu.hust.ddlsaver.domain.Importance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SaverDatabaseMigrationTest {
    @Test
    fun migratesV1FileAndKeepsData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Robolectric includes the test method in its temporary directory; keep this path short on Windows.
        val databaseName = "migration.db"
        val databasePath = context.getDatabasePath(databaseName)
        val parent = checkNotNull(databasePath.parentFile)
        check(parent.isDirectory || parent.mkdirs()) { "Cannot create migration test directory: $parent" }
        val clock = Clock.fixed(Instant.parse("2030-01-02T00:00:00Z"), ZoneOffset.UTC)
        var database: SaverDatabase? = null
        try {
            // Build a real file from the checked-in, unmodified version 1 schema rather than the v2 entities.
            val schemaPath = "schemas/cn.edu.hust.ddlsaver.data.SaverDatabase/1.json"
            val fixture = sequenceOf(File(schemaPath), File("app", schemaPath)).first { it.isFile }
            val schema = JSONObject(fixture.readText()).getJSONObject("database")
            SQLiteDatabase.openOrCreateDatabase(databasePath, null).use { legacy ->
                val entities = schema.getJSONArray("entities")
                for (index in 0 until entities.length()) {
                    val entity = entities.getJSONObject(index)
                    legacy.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                }
                val setup = schema.getJSONArray("setupQueries")
                for (index in 0 until setup.length()) legacy.execSQL(setup.getString(index))
                legacy.execSQL("INSERT INTO events VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf<Any?>("legacy-active", "原事项", clock.millis() + 60_000, 30, "原描述", "HIGH", null, null, 10L, 20L, 3L))
                legacy.execSQL("INSERT INTO events VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf<Any?>("legacy-closed", "已完成归档", clock.millis() - 60_000, null, "保留历史", "LOW", 30L, 40L, 11L, 50L, 6L))
                legacy.execSQL("INSERT INTO settings VALUES (1, 2880)")
                legacy.version = 1
            }
            fun open() = Room.databaseBuilder(context, SaverDatabase::class.java, databaseName)
                .addMigrations(SaverDatabase.MIGRATION_1_2).build()
            database = open()
            var repository = RoomEventRepository(database, clock)
            // Opening through Room executes the migration and validates every v2 table and index.
            val rows = repository.events.first().associateBy { it.id }
            val active = rows.getValue("legacy-active")
            assertEquals(2, rows.size)
            assertEquals("原事项", active.title)
            assertEquals("原描述", active.description)
            assertEquals(clock.millis() + 60_000, active.deadlineAt)
            assertEquals(30L, active.sprintMinutes)
            assertEquals(Importance.HIGH, active.importance)
            assertEquals(10L, active.createdAt)
            assertEquals(20L, active.updatedAt)
            assertEquals(3L, active.revision)
            assertNull(active.completedAt)
            assertNull(active.archivedAt)
            assertNull(active.source)
            assertTrue(active.tags.isEmpty() && active.customFields.isEmpty() && active.attachments.isEmpty())
            val closed = rows.getValue("legacy-closed")
            assertEquals(30L, closed.completedAt)
            assertEquals(40L, closed.archivedAt)
            assertEquals(6L, closed.revision)
            assertEquals(AppSettings(2880, ExpiredPolicy.KEEP), repository.settings.first())

            val source = repository.saveSource(null, "迁移后来源")
            val tag = repository.saveTag(null, "迁移后标签")
            val imageId = UUID.randomUUID().toString()
            val image = AttachmentRef(imageId, "$imageId.img", "image/png", "截图.png")
            val updated = repository.save(EventDraft(
                id = active.id, title = active.title, deadlineAt = active.deadlineAt,
                sprintMinutes = active.sprintMinutes, description = active.description,
                importance = active.importance, expectedRevision = active.revision,
                sourceId = source.id, tagIds = setOf(tag.id),
                customFields = listOf(CustomField("location", "地点", "东九")), attachments = listOf(image),
            ))
            database.close()
            database = open()
            repository = RoomEventRepository(database, clock)
            assertEquals(updated, repository.events.first().first { it.id == active.id })
            assertEquals(closed, repository.events.first().first { it.id == closed.id })
            assertEquals(setOf(image.relativePath), repository.referencedAttachmentPaths())
        } finally {
            database?.close()
            context.deleteDatabase(databaseName)
        }
    }
}
