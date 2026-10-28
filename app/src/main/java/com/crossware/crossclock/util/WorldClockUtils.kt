package com.crossware.crossclock.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 世界时钟时区、时间差、氛围描述及昼夜日出日落倒计时计算工具类。
 */
object WorldClockUtils {

    /**
     * 获取时区偏移字符串（如 GMT+9, GMT-4, GMT+0）
     */
    fun getTimeZoneOffsetString(zoneId: ZoneId, nowInstant: Instant = Instant.now()): String {
        val zdt = ZonedDateTime.ofInstant(nowInstant, zoneId)
        val offsetSeconds = zdt.offset.totalSeconds
        val hours = offsetSeconds / 3600
        val sign = if (hours >= 0) "+" else ""
        return "GMT$sign$hours"
    }

    /**
     * 计算某时区相对于系统本地时区的时间差描述（如 "慢 9 小时", "快 2 小时", "与本地同时区"）
     */
    fun getTimeDifferenceString(
        cityZoneId: ZoneId,
        localZoneId: ZoneId = ZoneId.systemDefault(),
        nowInstant: Instant = Instant.now()
    ): String {
        val cityZdt = ZonedDateTime.ofInstant(nowInstant, cityZoneId)
        val localZdt = ZonedDateTime.ofInstant(nowInstant, localZoneId)

        val cityOffsetSec = cityZdt.offset.totalSeconds
        val localOffsetSec = localZdt.offset.totalSeconds
        val diffHours = (cityOffsetSec - localOffsetSec) / 3600

        return when {
            diffHours == 0 -> "与本地同时区"
            diffHours > 0 -> "快 $diffHours 小时"
            else -> "慢 ${-diffHours} 小时"
        }
    }

    /**
     * 计算某时区日期相对于本地是“今天”、“昨天”还是“明天”
     */
    fun getRelativeDateString(
        cityZoneId: ZoneId,
        localZoneId: ZoneId = ZoneId.systemDefault(),
        nowInstant: Instant = Instant.now(),
        locale: Locale = Locale.getDefault()
    ): String {
        val cityZdt = ZonedDateTime.ofInstant(nowInstant, cityZoneId)
        val localZdt = ZonedDateTime.ofInstant(nowInstant, localZoneId)

        val cityDate = cityZdt.toLocalDate()
        val localDate = localZdt.toLocalDate()

        val dateFormatted = cityZdt.format(DateTimeFormatter.ofPattern("MM月dd日", locale))

        val dayDiff = ChronoUnit.DAYS.between(localDate, cityDate)
        val prefix = when (dayDiff) {
            0L -> "今天"
            -1L -> "昨天"
            1L -> "明天"
            else -> if (dayDiff < 0) "前$dateFormatted" else "后$dateFormatted"
        }

        return "$prefix · $dateFormatted"
    }

    /**
     * 获取时间段描述（已移除温度信息）与昼夜进度 (0.0..1.0)
     */
    fun getTimePeriodAndProgress(cityZoneId: ZoneId, nowInstant: Instant = Instant.now()): Pair<String, Float> {
        val zdt = ZonedDateTime.ofInstant(nowInstant, cityZoneId)
        val hour = zdt.hour
        val minute = zdt.minute
        val currentMin = hour * 60 + minute

        // 白天定义: 06:00 (360分) 至 18:00 (1080分)
        val isDay = currentMin in 360..1080

        val progress = if (isDay) {
            ((currentMin - 360) / 720f).coerceIn(0f, 1f)
        } else {
            val nightMin = if (currentMin > 1080) currentMin - 1080 else currentMin + 360
            (nightMin / 720f).coerceIn(0f, 1f)
        }

        val periodDesc = when (hour) {
            in 0..4 -> "静谧深夜"
            in 5..8 -> "清晨日光"
            in 9..11 -> "上午明媚"
            in 12..14 -> "正午日光"
            in 15..17 -> "下午温和"
            in 18..20 -> "黄昏 · 凉爽"
            else -> "深夜 · 晴朗"
        }

        return Pair(periodDesc, progress)
    }

    /**
     * 计算距离日落或日出的倒计时文本（白天算日落倒计时，夜间自动切换为日出倒计时）
     */
    fun getSunCountdownText(zoneId: ZoneId = ZoneId.systemDefault(), nowInstant: Instant = Instant.now()): String {
        val zdt = ZonedDateTime.ofInstant(nowInstant, zoneId)
        val hour = zdt.hour
        val minute = zdt.minute
        val currentMin = hour * 60 + minute

        val sunriseMin = 360  // 06:00
        val sunsetMin = 1080  // 18:00

        val isDay = currentMin in sunriseMin..sunsetMin

        return if (isDay) {
            val diffMin = sunsetMin - currentMin
            val h = diffMin / 60
            val m = diffMin % 60
            "距离日落 ${h}小时${m}分"
        } else {
            val diffMin = if (currentMin > sunsetMin) {
                (24 * 60 - currentMin) + sunriseMin
            } else {
                sunriseMin - currentMin
            }
            val h = diffMin / 60
            val m = diffMin % 60
            "距离日出 ${h}小时${m}分"
        }
    }
}
