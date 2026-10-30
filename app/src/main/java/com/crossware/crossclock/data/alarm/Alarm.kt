package com.crossware.crossclock.data.alarm

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.crossware.crossclock.R
import com.crossware.crossclock.data.PRESET_CITIES
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 跨时区世界闹钟实体类。
 *
 * @param id 主键 ID。
 * @param time 目标时区的设定时间（LocalDateTime）。
 * @param message 闹钟标题与标签。
 * @param timeZone 闹钟的目标唤醒时区。
 * @param onOrOff 是否开启。
 * @param ringtoneUri 铃声 URI 字符串。
 * @param ringtoneTitle 铃声显示标题。
 * @param repeatDays 重复日（如 "1,2,3,4,5"；为 null 表示单次固定日期）。
 * @param targetDate 单次闹钟的目标日期（如 "2027-12-13"）。
 * @param locationName 目标唤醒城市的关联名称（如 "Nagoya", "名古屋", "San Jose"）。
 */
@Entity
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val time: LocalDateTime,
    val message: String,
    val timeZone: ZoneId,
    val onOrOff: Boolean = true,
    val ringtoneUri: String? = null,
    val ringtoneTitle: String? = null,
    val repeatDays: String? = null,
    val targetDate: String? = null,
    val locationName: String? = null
) {
    /**
     * 获取用于显示的唤醒城市/时区名称（防止同属 Asia/Tokyo 的 Nagoya/Osaka 被误替换为 Tokyo）。
     */
    fun getDisplayZoneCityName(locale: Locale = Locale.getDefault()): String {
        if (!locationName.isNullOrBlank()) {
            val rawName = locationName.trim()
            val matchedPreset = PRESET_CITIES.find { preset ->
                preset.zoneId == timeZone && (
                    preset.englishName.equals(rawName, ignoreCase = true) ||
                    preset.nameZhCN.equals(rawName, ignoreCase = true) ||
                    preset.nameZhTW.equals(rawName, ignoreCase = true) ||
                    preset.nameJa.equals(rawName, ignoreCase = true) ||
                    rawName.contains(preset.englishName, ignoreCase = true) ||
                    rawName.contains(preset.nameZhCN, ignoreCase = true) ||
                    rawName.contains(preset.nameZhTW, ignoreCase = true) ||
                    rawName.contains(preset.nameJa, ignoreCase = true)
                )
            }
            return matchedPreset?.getLocalizedCityName(locale) ?: rawName
        }

        val presetCity = PRESET_CITIES.find { it.zoneId == timeZone }
        return presetCity?.getLocalizedCityName(locale) ?: timeZone.id
    }

    /**
     * 解析重复天数列表 (1=Mon, 2=Tue, ..., 7=Sun)
     */
    fun parseRepeatDaysList(): List<Int> {
        if (repeatDays.isNullOrBlank()) return emptyList()
        return repeatDays.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    /**
     * 基于目标时区 (timeZone) 计算下一次准确触发的 ZonedDateTime。
     */
    fun getNextTriggerZonedDateTime(): ZonedDateTime {
        val targetZone = timeZone
        val targetTime = time.toLocalTime()
        val nowInTargetZone = ZonedDateTime.now(targetZone)

        val daysList = parseRepeatDaysList()

        if (daysList.isEmpty()) {
            val date = if (!targetDate.isNullOrBlank()) {
                try {
                    LocalDate.parse(targetDate)
                } catch (e: Exception) {
                    time.toLocalDate()
                }
            } else {
                time.toLocalDate()
            }

            var candidateZdt = ZonedDateTime.of(date, targetTime, targetZone)
            if (targetDate.isNullOrBlank() && candidateZdt.isBefore(nowInTargetZone)) {
                candidateZdt = candidateZdt.plusDays(1)
            }
            return candidateZdt
        } else {
            var candidateDate = nowInTargetZone.toLocalDate()
            var candidateZdt = ZonedDateTime.of(candidateDate, targetTime, targetZone)

            for (i in 0..14) {
                val dayOfWeekVal = candidateDate.dayOfWeek.value
                if (daysList.contains(dayOfWeekVal)) {
                    if (candidateZdt.isAfter(nowInTargetZone)) {
                        return candidateZdt
                    }
                }
                candidateDate = candidateDate.plusDays(1)
                candidateZdt = ZonedDateTime.of(candidateDate, targetTime, targetZone)
            }
            return candidateZdt
        }
    }

    /**
     * 单次固定日期闹钟是否已过期。
     */
    fun isExpired(): Boolean {
        if (!repeatDays.isNullOrBlank()) return false
        val nextZdt = getNextTriggerZonedDateTime()
        val nowInTargetZone = ZonedDateTime.now(timeZone)
        return nextZdt.isBefore(nowInTargetZone)
    }

    /**
     * 获取对应用户设备本地时间的字符串 (如 "18:30")。
     */
    fun getLocalEquivalentTimeString(): String {
        val nextZdt = getNextTriggerZonedDateTime()
        val localZdt = nextZdt.withZoneSameInstant(ZoneId.systemDefault())
        return localZdt.format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    /**
     * 获取距离响铃的剩余倒计时文本（全语种本地化）。
     */
    fun getTimeRemainingText(context: Context): String {
        if (!onOrOff) return context.getString(R.string.paused_status)
        if (isExpired()) return context.getString(R.string.expired_completed)

        val nextZdt = getNextTriggerZonedDateTime()
        val now = ZonedDateTime.now(timeZone)

        val totalMinutes = ChronoUnit.MINUTES.between(now, nextZdt)
        if (totalMinutes < 0) return context.getString(R.string.expired_completed)
        if (totalMinutes < 1) return context.getString(R.string.rings_soon)

        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return if (hours >= 24) {
            val days = hours / 24
            context.getString(R.string.rings_in_days_format, days)
        } else if (hours > 0) {
            context.getString(R.string.rings_in_hours_format, hours)
        } else {
            context.getString(R.string.rings_in_minutes_format, minutes)
        }
    }

    /**
     * 获取重复周期的描述文本（全语种本地化）。
     */
    fun getRepeatDisplayText(context: Context): String {
        val days = parseRepeatDaysList()
        val singleRunText = context.getString(R.string.single_run)
        if (days.isEmpty()) {
            return if (!targetDate.isNullOrBlank()) {
                try {
                    val date = LocalDate.parse(targetDate)
                    "${date.monthValue}/${date.dayOfMonth} · $singleRunText"
                } catch (e: Exception) {
                    singleRunText
                }
            } else {
                singleRunText
            }
        }

        if (days.size == 7) return context.getString(R.string.everyday)
        if (days.sorted() == listOf(1, 2, 3, 4, 5)) return context.getString(R.string.workdays)
        if (days.sorted() == listOf(6, 7)) return context.getString(R.string.weekends)

        val locale = Locale.getDefault()
        val dayNames = if (locale.language == "zh") {
            mapOf(1 to "周一", 2 to "周二", 3 to "周三", 4 to "周四", 5 to "周五", 6 to "周六", 7 to "周日")
        } else if (locale.language == "ja") {
            mapOf(1 to "月", 2 to "火", 3 to "水", 4 to "木", 5 to "金", 6 to "土", 7 to "日")
        } else {
            mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
        }
        val selectedNames = days.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
        return selectedNames
    }
}
