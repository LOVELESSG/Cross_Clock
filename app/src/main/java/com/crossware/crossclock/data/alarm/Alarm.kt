package com.crossware.crossclock.data.alarm

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
    val targetDate: String? = null
) {
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
            // 固定单次日期闹钟
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
            // 固定日重复闹钟（如每周一、三、五，基于目标时区计算）
            var candidateDate = nowInTargetZone.toLocalDate()
            var candidateZdt = ZonedDateTime.of(candidateDate, targetTime, targetZone)

            for (i in 0..14) {
                val dayOfWeekVal = candidateDate.dayOfWeek.value // 1=Mon, ..., 7=Sun
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
     * 获取距离响铃的剩余倒计时文本（如 "18小时后响铃"）。
     */
    fun getTimeRemainingText(): String {
        if (!onOrOff) return "已休眠暂停"
        if (isExpired()) return "已逾期完成"

        val nextZdt = getNextTriggerZonedDateTime()
        val now = ZonedDateTime.now(timeZone)

        val totalMinutes = ChronoUnit.MINUTES.between(now, nextZdt)
        if (totalMinutes < 0) return "已逾期完成"
        if (totalMinutes < 1) return "即将响铃"

        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return if (hours >= 24) {
            val days = hours / 24
            "${days}天后响铃"
        } else if (hours > 0) {
            "${hours}小时后响铃"
        } else {
            "${minutes}分钟后响铃"
        }
    }

    /**
     * 获取重复周期的描述文本 (如 "工作日 (周一至周五)", "每周二、四", "每天", "10月18日 · 单次")。
     */
    fun getRepeatDisplayText(): String {
        val days = parseRepeatDaysList()
        if (days.isEmpty()) {
            return if (!targetDate.isNullOrBlank()) {
                try {
                    val date = LocalDate.parse(targetDate)
                    "${date.monthValue}月${date.dayOfMonth}日 · 单次"
                } catch (e: Exception) {
                    "单次"
                }
            } else {
                "单次"
            }
        }

        if (days.size == 7) return "每天"
        if (days.sorted() == listOf(1, 2, 3, 4, 5)) return "工作日 (周一至周五)"
        if (days.sorted() == listOf(6, 7)) return "周末"

        val dayNames = mapOf(
            1 to "一", 2 to "二", 3 to "三", 4 to "四",
            5 to "五", 6 to "六", 7 to "日"
        )
        val selectedNames = days.sorted().mapNotNull { dayNames[it] }.joinToString("、")
        return "每周$selectedNames"
    }
}
