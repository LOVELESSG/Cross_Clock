package com.example.crossclock.data.alarm

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Insert
    suspend fun insertAlarm(alarm: Alarm): Long

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Query("select * from Alarm")
    fun loadAllAlarm(): Flow<List<Alarm>>

    @Delete
    suspend fun deleteAlarm(alarm: Alarm)

    /*@Query("delete from Alarm where message = :message")
    suspend fun deleteAlarmByMessage(message: String)*/

    /*@Query("select exists(select 1 from Alarm where message = :message)")
    fun existsAlarm(message: String): Flow<Boolean>*/
}