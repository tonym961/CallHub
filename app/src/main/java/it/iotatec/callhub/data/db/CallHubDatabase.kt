package it.iotatec.callhub.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CallEventEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class CallHubDatabase : RoomDatabase() {
    abstract fun callEventDao(): CallEventDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE call_events ADD COLUMN note TEXT")
            }
        }

        @Volatile private var instance: CallHubDatabase? = null

        fun get(context: Context): CallHubDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CallHubDatabase::class.java,
                    "callhub.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
