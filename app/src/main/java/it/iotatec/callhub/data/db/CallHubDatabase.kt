package it.iotatec.callhub.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [CallEventEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class CallHubDatabase : RoomDatabase() {
    abstract fun callEventDao(): CallEventDao

    companion object {
        @Volatile private var instance: CallHubDatabase? = null

        fun get(context: Context): CallHubDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CallHubDatabase::class.java,
                    "callhub.db"
                ).build().also { instance = it }
            }
    }
}
