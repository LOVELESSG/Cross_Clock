package com.example.crossclock.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.crossclock.data.alarm.Alarm
import com.example.crossclock.data.alarm.AlarmDao
import com.example.crossclock.data.converter.LocalDateTimeConverter
import com.example.crossclock.data.converter.ZoneIdConverter
import com.example.crossclock.data.worldclock.WorldClock
import com.example.crossclock.data.worldclock.WorldClockDao

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