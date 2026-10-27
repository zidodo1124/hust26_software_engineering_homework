package cn.edu.hust.ddlsaver.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [EventEntity::class, SettingsEntity::class], version = 1, exportSchema = true)
@TypeConverters(SaverConverters::class)
abstract class SaverDatabase : RoomDatabase() {
    abstract fun dao(): SaverDao

    companion object {
        fun create(context: Context): SaverDatabase = Room.databaseBuilder(
            context.applicationContext,
            SaverDatabase::class.java,
            "ddl-saver.db",
        ).build()
    }
}
