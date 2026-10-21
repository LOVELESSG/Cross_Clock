package com.crossware.crossclock.data.worldclock

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.crossware.crossclock.util.CitySearchResult
import java.time.ZoneId

/**
 * 表示世界时钟的实体类，用于 Room 数据库存储。
 *
 * @param city 城市全名或地理位置描述字符串。
 * @param cityTimeZoneId 该城市对应的时区 ID (java.time.ZoneId)。
 * @param country 国家或地区名称。
 * @param id 数据库主键，自动生成。
 */
@Entity
data class WorldClock (
    val city: String,
    val cityTimeZoneId: ZoneId,
    val country: String? = null,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0
) {
    private val rawParts: List<String>
        get() = city.split(",").map { it.trim() }.filter { it.isNotBlank() }

    /**
     * 获取用于显示的城市基础名称（如 "Springfield", "Tokyo", "Baotou"）。
     */
    fun getDisplayCityName(): String {
        return rawParts.firstOrNull() ?: city.trim()
    }

    /**
     * 获取国家或地区名称（优先返回 country 字段，如果为空则解析 city 字符串）。
     */
    fun getDisplayCountry(): String? {
        if (!country.isNullOrBlank()) return country.trim()
        return if (rawParts.size >= 2) rawParts.last() else null
    }

    /**
     * 获取省份/州/一级行政区名称（如果有）。
     */
    fun getDisplayAdminArea(): String? {
        if (rawParts.size >= 3) {
            return rawParts[1]
        }
        if (rawParts.size == 2) {
            val displayCountry = getDisplayCountry()
            if (!displayCountry.isNullOrBlank() && !rawParts.last().equals(displayCountry, ignoreCase = true)) {
                return rawParts[1]
            }
        }
        return null
    }

    /**
     * 判断当前已保存的时钟条目是否与指定的搜索结果匹配。
     * 精准校验时区、基础城市名、省/州行政区及国家名称，避免不同行政区的同名城市发生误勾选或覆盖删除。
     */
    fun isMatchingSearchResult(searchResult: CitySearchResult): Boolean {
        // 1. 时区必须一致
        if (this.cityTimeZoneId != searchResult.zoneId) return false

        // 2. 完整字符串完全匹配
        if (this.city.equals(searchResult.displayLocationName, ignoreCase = true)) return true

        // 3. 基础城市名匹配
        val thisCityName = getDisplayCityName()
        if (!thisCityName.equals(searchResult.cityName, ignoreCase = true)) return false

        // 4. 省/州/一级行政区匹配（若双方均有值且不同，则不匹配）
        val thisAdmin = getDisplayAdminArea()
        val searchAdmin = searchResult.adminArea
        if (!thisAdmin.isNullOrBlank() && !searchAdmin.isNullOrBlank() && !thisAdmin.equals(searchAdmin, ignoreCase = true)) {
            return false
        }

        // 5. 国家/地区匹配（若双方均有值且不同，则不匹配）
        val thisCountry = getDisplayCountry()
        val searchCountry = searchResult.country
        if (!thisCountry.isNullOrBlank() && !searchCountry.isNullOrBlank() && !thisCountry.equals(searchCountry, ignoreCase = true)) {
            return false
        }

        return true
    }
}
