package com.crossware.crossclock.di

import android.content.Context
import com.crossware.crossclock.data.AppDatabase
import com.crossware.crossclock.data.Repository
import com.crossware.crossclock.data.alarm.AlarmDao
import com.crossware.crossclock.data.timer.TimerDao
import com.crossware.crossclock.data.worldclock.WorldClockDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideWorldClockDao(appDatabase: AppDatabase): WorldClockDao {
        return appDatabase.worldClockDao()
    }

    @Provides
    fun provideAlarmDao(appDatabase: AppDatabase): AlarmDao {
        return appDatabase.alarmDao()
    }

    @Provides
    fun provideTimerDao(appDatabase: AppDatabase): TimerDao {
        return appDatabase.timerDao()
    }

    @Singleton
    @Provides
    fun provideRepository(
        worldClockDao: WorldClockDao,
        alarmDao: AlarmDao,
        timerDao: TimerDao
    ): Repository {
        return Repository(worldClockDao, alarmDao, timerDao)
    }
}
