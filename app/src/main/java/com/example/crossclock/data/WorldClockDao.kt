package com.example.crossclock.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.*

@Dao
interface WorldClockDao {

    @Insert
    fun insertWorldClock(worldClock: WorldClock): Long

    @Update
    fun updateWorldClock(worldClock: WorldClock)

    @Query("select * from WorldClock")
    fun loadAllWorldClock(): Flow<List<WorldClock>>

    @Delete
    fun deleteWorldClock(worldClock: WorldClock)

    @Query("delete from WorldClock where city = :cityName")
    fun deleteWorldClockByCityName(cityName: String): Int

    @Query("select exists(select 1 from WorldClock where city = :cityName)")
    fun existsWorldClock(cityName: String): Boolean
}