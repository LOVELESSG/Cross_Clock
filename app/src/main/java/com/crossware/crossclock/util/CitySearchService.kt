package com.crossware.crossclock.util

import com.crossware.crossclock.data.ALL_CITIES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * 城市与时区搜索结果模型。
 *
 * @param cityName 城市名称（如 "Halifax", "Tokyo"）。
 * @param country 国家或地区名称（如 "Canada"）。
 * @param adminArea 省/州/一级行政区名称（如 "Nova Scotia"）。
 * @param zoneId 对应的标准 IANA 时区 ID（如 ZoneId.of("America/Halifax")）。
 * @param isOnline 是否来自于在线 Geocoding API 搜索结果。
 */
data class CitySearchResult(
    val cityName: String,
    val country: String? = null,
    val adminArea: String? = null,
    val zoneId: ZoneId,
    val isOnline: Boolean = false
) {
    /**
     * 用于 UI 显示的格式化文本。
     * 例如："Halifax, Nova Scotia, Canada (Atlantic Standard Time)"
     */
    val displayLocationName: String
        get() {
            val locationParts = listOfNotNull(cityName, adminArea, country)
                .filter { it.isNotBlank() }
                .distinct()
            return locationParts.joinToString(", ")
        }

    val displayTimeZoneName: String
        get() {
            return try {
                zoneId.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
            } catch (e: Exception) {
                zoneId.id
            }
        }
}

/**
 * 混合模式城市与时区搜索服务。
 * 提供本地预设城市列表过滤，以及通过 Open-Meteo 免费 API 在线搜索全球城市及确定时区的功能。
 */
object CitySearchService {

    /**
     * 本地预设城市模糊匹配。
     */
    fun searchLocal(query: String): List<CitySearchResult> {
        val trimmed = query.trim()
        val results = if (trimmed.isEmpty()) {
            ALL_CITIES.map {
                CitySearchResult(
                    cityName = it.city,
                    zoneId = it.cityTimeZoneId,
                    isOnline = false
                )
            }
        } else {
            val lowerQuery = trimmed.lowercase()
            ALL_CITIES.filter {
                it.city.lowercase().contains(lowerQuery) ||
                        it.cityTimeZoneId.id.lowercase().contains(lowerQuery)
            }.map {
                CitySearchResult(
                    cityName = it.city,
                    zoneId = it.cityTimeZoneId,
                    isOnline = false
                )
            }
        }
        return results.distinctBy { "${it.displayLocationName}_${it.zoneId.id}" }
    }

    /**
     * 在线搜索全球城市及关联的 IANA 时区（Open-Meteo 免费 API，免 Key）。
     */
    suspend fun searchOnline(query: String): List<CitySearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val language = if (Locale.getDefault().language == "zh") "zh" else "en"
            val urlString = "https://geocoding-api.open-meteo.com/v1/search?name=$encodedQuery&count=10&language=$language"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(responseText)
                val resultsArray = jsonObject.optJSONArray("results") ?: return@withContext emptyList()

                val results = mutableListOf<CitySearchResult>()
                for (i in 0 until resultsArray.length()) {
                    val item = resultsArray.getJSONObject(i)
                    val name = item.optString("name", "")
                    val country = item.optString("country", "")
                    val admin1 = item.optString("admin1", "")
                    val timezoneStr = item.optString("timezone", "")
                    val featureCode = item.optString("feature_code", "")

                    // 仅保留市级及以上居民点/城市单位 (feature_code 以 PPL 开头，如 PPLC=首都, PPLA=省会/地级市, PPL=城市/城镇)
                    // 排除国家 (PCLI) 和省/州 (ADM1) 等非市级实体
                    val isCityLevel = featureCode.isEmpty() || featureCode.startsWith("PPL")

                    if (isCityLevel && name.isNotBlank() && timezoneStr.isNotBlank()) {
                        val zoneId = try {
                            ZoneId.of(timezoneStr)
                        } catch (e: Exception) {
                            null
                        }
                        if (zoneId != null) {
                            results.add(
                                CitySearchResult(
                                    cityName = name,
                                    country = country.ifBlank { null },
                                    adminArea = admin1.ifBlank { null },
                                    zoneId = zoneId,
                                    isOnline = true
                                )
                            )
                        }
                    }
                }
                results.distinctBy { "${it.displayLocationName}_${it.zoneId.id}" }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
