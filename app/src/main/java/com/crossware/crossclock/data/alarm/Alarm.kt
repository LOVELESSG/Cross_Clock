package com.crossware.crossclock.data.alarm

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 闹钟数据实体类，映射到 Room 数据库中的 'Alarm' 表。
 * 
 * @param id 唯一标识符，自动生成。
 * @param time 闹钟设定的日期和时间（LocalDateTime）。
 * @param message 闹钟的备注或标签。
 * @param timeZone 闹钟关联的时区 ID。
 * @param onOrOff 闹钟当前是否启用。
 */
@Entity
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val time: LocalDateTime,
    val message: String,
    val timeZone: ZoneId,
    val onOrOff: Boolean = true
)
