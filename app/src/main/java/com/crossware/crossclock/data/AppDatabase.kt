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

/**
 * 应用的主数据库类，使用 Room 持久化库。
 * 包含 'WorldClock' 和 'Alarm' 两个实体表。
 */
@Database(entities = [WorldClock::class, Alarm::class], version = 1, exportSchema = false)
@TypeConverters(ZoneIdConverter::class, LocalDateTimeConverter::class)
abstract class AppDatabase: RoomDatabase() {

    /** 获取世界时钟的 DAO */
    abstract fun worldClockDao(): WorldClockDao
    
    /** 获取闹钟的 DAO */
    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        /**
         * 获取数据库单例。
         * 使用双重检查锁定确保线程安全且仅初始化一次。
         */
        fun getDatabase(context: Context): AppDatabase{
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "app_database")
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
