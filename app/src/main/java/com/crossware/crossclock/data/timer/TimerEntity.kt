package com.crossware.crossclock.data.timer

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 表示计时器的 Room 数据库实体类。
 *
 * @param id 主键 ID，自动生成。
 * @param name 计时器组标题/名称（如 "番茄工作法", "健身训练"）。
 * @param type 计时器类型（SINGLE, LINEAR, LOOP）。
 * @param loopCount 循环次数（仅当 type == LOOP 时有效；-1 表示无限循环）。
 * @param steps 串联的单独计时器步骤列表。
 */
@Entity(tableName = "Timer")
data class TimerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: TimerType,
    val loopCount: Int = 1,
    val steps: List<TimerStep>
) {
    /**
     * 计算单个循环周期的总时长（秒）。
     */
    val totalSingleCycleDurationSeconds: Long
        get() = steps.sumOf { it.durationSeconds }

    /**
     * 计算所有周期的总时长（秒）。如果是无限循环则返回单个周期时长。
     */
    val totalDurationSeconds: Long
        get() = if (type == TimerType.LOOP && loopCount > 0) {
            totalSingleCycleDurationSeconds * loopCount
        } else {
            totalSingleCycleDurationSeconds
        }
}
