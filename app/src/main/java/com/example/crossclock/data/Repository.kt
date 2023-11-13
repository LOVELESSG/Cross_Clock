package com.example.crossclock.data

import com.example.crossclock.data.alarm.Alarm
import com.example.crossclock.data.alarm.AlarmDao
import com.example.crossclock.data.worldclock.WorldClock
import com.example.crossclock.data.worldclock.WorldClockDao

class Repository(
    private val worldClockDao: WorldClockDao,
    private val alarmDao: AlarmDao
) {
    //World Clock part
    val allWorldClock = worldClockDao.loadAllWorldClock()

    suspend fun insertWorldClock(worldClock: WorldClock){
        worldClockDao.insertWorldClock(worldClock)
    }

    suspend fun updateWorldClock(worldClock: WorldClock){
        worldClockDao.updateWorldClock(worldClock)
    }

    suspend fun deleteWorldClock(worldClock: WorldClock){
        worldClockDao.deleteWorldClock(worldClock)
    }

    suspend fun deleteWorldClockByName(cityName: String) {
        worldClockDao.deleteWorldClockByCityName(cityName)
    }

    fun existsWorldClock(cityName: String) = worldClockDao.existsWorldClock(cityName)

    // Alarm part
    val allAlarm = alarmDao.loadAllAlarm()

    suspend fun insertAlarm(alarm: Alarm) {
        alarmDao.insertAlarm(alarm)
    }

    suspend fun updateAlarm(alarm: Alarm) {
        alarmDao.updateAlarm(alarm)
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        alarmDao.deleteAlarm(alarm)
    }
}