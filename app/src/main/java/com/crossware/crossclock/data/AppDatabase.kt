package com.crossware.crossclock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.crossware.crossclock.data.alarm.Alarm
import com.crossware.crossclock.data.alarm.AlarmDao
import com.crossware.crossclock.data.converter.LocalDateTimeConverter
import com.crossware.crossclock.data.converter.ZoneIdConverter
import com.crossware.crossclock.data.worldclock.WorldClock
import com.crossware.crossclock.data.worldclock.WorldClockDao

@Database(entities = [WorldClock::class, Alarm::class], version = 1, exportSchema = false)
@TypeConverters(ZoneIdConverter::class, LocalDateTimeConverter::class)
abstract class AppDatabase: RoomDatabase() {

    abstract fun worldClockDao(): WorldClockDao
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase{
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                    .build()
                    .also { Instance = it }
            }
        }
    }
}