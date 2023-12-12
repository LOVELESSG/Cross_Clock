package com.crossware.crossclock.data

import com.crossware.crossclock.data.alarm.Alarm
import com.crossware.crossclock.data.alarm.AlarmDao
import com.crossware.crossclock.data.worldclock.WorldClock
import com.crossware.crossclock.data.worldclock.WorldClockDao

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

    suspend fun updateAlarmStatus(alarm: Alarm) {
        alarmDao.updateAlarmStatus(alarm.id, !alarm.onOrOff)
    }

    suspend fun deleteAlarm(alarm: Alarm) {
        alarmDao.deleteAlarm(alarm)
    }
}