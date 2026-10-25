package com.crossware.crossclock.data.timer

import java.util.UUID

/**
 * 计时器中的单独步骤/子计时器数据模型。
 *
 * @param id 唯一标识符。
 * @param name 步骤名称（如 "工作", "休息", "热身"）。
 * @param durationSeconds 计时时长（秒）。
 * @param colorHex 卡片背景颜色十六进制字符串（如 "#FF5722"）。
 */
data class TimerStep(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val durationSeconds: Long,
    val colorHex: String = "#3F51B5"
)
