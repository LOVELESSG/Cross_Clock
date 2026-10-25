package com.crossware.crossclock.data.timer

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerDao {

    @Insert
    suspend fun insertTimer(timer: TimerEntity): Long

    @Update
    suspend fun updateTimer(timer: TimerEntity)

    @Delete
    suspend fun deleteTimer(timer: TimerEntity)

    @Query("SELECT * FROM Timer ORDER BY id DESC")
    fun loadAllTimers(): Flow<List<TimerEntity>>

    @Query("SELECT * FROM Timer WHERE id = :id")
    fun getTimerById(id: Long): Flow<TimerEntity?>
}
