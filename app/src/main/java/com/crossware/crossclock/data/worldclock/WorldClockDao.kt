package com.crossware.crossclock.data.worldclock

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.*

@Dao
interface WorldClockDao {

    @Insert
    suspend fun insertWorldClock(worldClock: WorldClock): Long

    @Update
    suspend fun updateWorldClock(worldClock: WorldClock)

    @Query("select * from WorldClock")
    fun loadAllWorldClock(): Flow<List<WorldClock>>

    @Delete
    suspend fun deleteWorldClock(worldClock: WorldClock)

    @Query("delete from WorldClock where city = :cityName")
    suspend fun deleteWorldClockByCityName(cityName: String)

    @Query("select exists(select 1 from WorldClock where city = :cityName)")
    fun existsWorldClock(cityName: String): Flow<Boolean>
}