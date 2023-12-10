package com.example.crossclock.data.alarm

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Insert
    suspend fun insertAlarm(alarm: Alarm): Long

    @Query("update Alarm set onOrOff = :onOrOff where id = :id")
    suspend fun updateAlarmStatus(id:Int, onOrOff: Boolean)

    @Query("select * from Alarm")
    fun loadAllAlarm(): Flow<List<Alarm>>

    @Query("select * from Alarm")
    fun loadAllAlarmInList(): List<Alarm>

    @Delete
    suspend fun deleteAlarm(alarm: Alarm)

    /*@Query("delete from Alarm where message = :message")
    suspend fun deleteAlarmByMessage(message: String)*/

    /*@Query("select exists(select 1 from Alarm where message = :message)")
    fun existsAlarm(message: String): Flow<Boolean>*/
}