package com.crossware.crossclock.data.worldclock

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.ZoneId

/**
 * 表示世界时钟的实体类，用于 Room 数据库存储。
 *
 * @param city 城市名称。
 * @param cityTimeZoneId 该城市对应的时区 ID (java.time.ZoneId)。
 * @param id 数据库主键，自动生成。
 */
@Entity
data class WorldClock (
    val city: String,
    val cityTimeZoneId: ZoneId,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0
)
