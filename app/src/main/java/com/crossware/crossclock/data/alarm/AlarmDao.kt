package com.crossware.crossclock.data.alarm

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 闹钟数据访问对象 (DAO)，定义了对 'Alarm' 表的所有 SQL 操作。
 */
@Dao
interface AlarmDao {

    /**
     * 插入一条新闹钟记录。
     * @return 插入记录生成的 ID。
     */
    @Insert
    suspend fun insertAlarm(alarm: Alarm): Long

    /**
     * 更新已有闹钟的所有字段。
     */
    @Update
    suspend fun updateAlarm(alarm: Alarm)

    /**
     * 仅更新闹钟的开关状态字段。
     * 
     * @param id 闹钟 ID。
     * @param onOrOff 目标开关状态。
     */
    @Query("update Alarm set onOrOff = :onOrOff where id = :id")
    suspend fun updateAlarmStatus(id: Int, onOrOff: Boolean)

    /**
     * 获取所有闹钟记录，并以流 (Flow) 的形式返回。
     * 当数据表发生变化时，流会自动发出新的列表。
     */
    @Query("select * from Alarm")
    fun loadAllAlarm(): Flow<List<Alarm>>

    /**
     * 以常规列表形式一次性加载所有闹钟（非响应式）。
     */
    @Query("select * from Alarm")
    fun loadAllAlarmInList(): List<Alarm>

    /**
     * 从数据库中删除指定的闹钟记录。
     */
    @Delete
    suspend fun deleteAlarm(alarm: Alarm)
}
