package com.crossware.crossclock.data.stopwatch

/**
 * 秒表分圈/计次数据模型。
 *
 * @param lapIndex 圈次（例如 1, 2, 3...）
 * @param lapTimeMillis 该单圈耗时（毫秒）
 * @param totalTimeMillis 记录该圈时的总累计耗时（毫秒）
 * @param isBest 是否为最佳单圈（最少用时）
 */
data class LapItem(
    val lapIndex: Int,
    val lapTimeMillis: Long,
    val totalTimeMillis: Long,
    val isBest: Boolean = false
)
