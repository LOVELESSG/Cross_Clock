package com.crossware.crossclock.data.timer

/**
 * 计时器到期提示模式。
 * - SOUND_AND_VIBRATION: 响铃与震动
 * - SOUND_ONLY: 仅响铃
 * - VIBRATION_ONLY: 仅震动
 * - NONE: 无提示（静音与不震动）
 */
enum class AlertMode {
    SOUND_AND_VIBRATION,
    SOUND_ONLY,
    VIBRATION_ONLY,
    NONE
}
