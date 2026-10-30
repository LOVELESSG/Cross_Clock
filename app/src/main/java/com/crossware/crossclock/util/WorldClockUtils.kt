package com.crossware.crossclock.util

import android.content.Context
import com.crossware.crossclock.R
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 世界时钟时区、时间差、氛围描述及昼夜日出日落倒计时计算工具类（全语种 Resource 本地化支持）。
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
     * 计算某时区相对于系统本地时区的时间差描述（支持 Resource 本地化，如 "快 9 小时", "慢 9 小时", "9 hours faster"）
     */
    fun getTimeDifferenceString(
        context: Context,
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
            diffHours == 0 -> context.getString(R.string.same_timezone_as_local)
            diffHours > 0 -> context.getString(R.string.hours_faster, diffHours)
            else -> context.getString(R.string.hours_slower, -diffHours)
        }
    }

    /**
     * 计算某时区日期相对于本地是“今天”、“昨天”还是“明天”
     */
    fun getRelativeDateString(
        context: Context,
        cityZoneId: ZoneId,
        localZoneId: ZoneId = ZoneId.systemDefault(),
        nowInstant: Instant = Instant.now(),
        locale: Locale = Locale.getDefault()
    ): String {
        val cityZdt = ZonedDateTime.ofInstant(nowInstant, cityZoneId)
        val localZdt = ZonedDateTime.ofInstant(nowInstant, localZoneId)

        val cityDate = cityZdt.toLocalDate()
        val localDate = localZdt.toLocalDate()

        val dateFormatted = cityZdt.format(DateTimeFormatter.ofPattern("MM/dd", locale))

        val dayDiff = ChronoUnit.DAYS.between(localDate, cityDate)
        val prefix = when (dayDiff) {
            0L -> context.getString(R.string.today)
            -1L -> context.getString(R.string.yesterday)
            1L -> context.getString(R.string.tomorrow)
            else -> dateFormatted
        }

        return "$prefix · $dateFormatted"
    }

    /**
     * 获取时间段描述（支持 Resource 本地化）与昼夜进度 (0.0..1.0)
     */
    fun getTimePeriodAndProgress(
        context: Context,
        cityZoneId: ZoneId,
        nowInstant: Instant = Instant.now()
    ): Pair<String, Float> {
        val zdt = ZonedDateTime.ofInstant(nowInstant, cityZoneId)
        val hour = zdt.hour
        val minute = zdt.minute
        val currentMin = hour * 60 + minute

        val isDay = currentMin in 360..1080

        val progress = if (isDay) {
            ((currentMin - 360) / 720f).coerceIn(0f, 1f)
        } else {
            val nightMin = if (currentMin > 1080) currentMin - 1080 else currentMin + 360
            (nightMin / 720f).coerceIn(0f, 1f)
        }

        val periodDesc = when (hour) {
            in 0..4 -> context.getString(R.string.quiet_midnight)
            in 5..8 -> context.getString(R.string.morning_daylight)
            in 9..11 -> context.getString(R.string.morning_bright)
            in 12..14 -> context.getString(R.string.midday_sunlight)
            in 15..17 -> context.getString(R.string.afternoon_gentle)
            in 18..20 -> context.getString(R.string.dusk_cool)
            else -> context.getString(R.string.night_clear)
        }

        return Pair(periodDesc, progress)
    }

    /**
     * 计算距离日落或日出的倒计时文本（支持 Resource 本地化）
     */
    fun getSunCountdownText(
        context: Context,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowInstant: Instant = Instant.now()
    ): String {
        val zdt = ZonedDateTime.ofInstant(nowInstant, zoneId)
        val hour = zdt.hour
        val minute = zdt.minute
        val currentMin = hour * 60 + minute

        val sunriseMin = 360
        val sunsetMin = 1080

        val isDay = currentMin in sunriseMin..sunsetMin

        return if (isDay) {
            val diffMin = sunsetMin - currentMin
            val h = diffMin / 60
            val m = diffMin % 60
            context.getString(R.string.sunset_countdown, h, m)
        } else {
            val diffMin = if (currentMin > sunsetMin) {
                (24 * 60 - currentMin) + sunriseMin
            } else {
                sunriseMin - currentMin
            }
            val h = diffMin / 60
            val m = diffMin % 60
            context.getString(R.string.sunrise_countdown, h, m)
        }
    }
}
