package com.crossware.crossclock.data.timer

import androidx.annotation.Keep

/**
 * 计时器类型枚举。
 * - SINGLE: 单独计时器（仅包含单一时间段）
 * - LINEAR: 线性计时器（多个单独计时器串联，自动依次顺序触发）
 * - LOOP: 循环计时器（线性计时器首尾相连，可设定循环次数或无限循环）
 */
@Keep
enum class TimerType {
    SINGLE,
    LINEAR,
    LOOP
}
