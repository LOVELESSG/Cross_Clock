package com.crossware.crossclock.util

import com.crossware.crossclock.data.PRESET_CITIES
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
 */
data class CitySearchResult(
    val cityName: String,
    val country: String? = null,
    val adminArea: String? = null,
    val zoneId: ZoneId,
    val isOnline: Boolean = false
) {
    /**
     * 用于 UI 显示的格式化文本（精准去重，格式为 "城市名, 省份, 国家"）。
     */
    val displayLocationName: String
        get() {
            val locCity = cityName.trim()
            val locAdmin = adminArea?.trim()
            val locCountry = country?.trim()

            val parts = mutableListOf<String>()
            if (locCity.isNotBlank()) {
                parts.add(locCity)
            }
            if (!locAdmin.isNullOrBlank() && !locAdmin.equals(locCity, ignoreCase = true)) {
                parts.add(locAdmin)
            }
            if (!locCountry.isNullOrBlank() && !locCountry.equals(locCity, ignoreCase = true) && !locCountry.equals(locAdmin, ignoreCase = true)) {
                parts.add(locCountry)
            }

            return parts.joinToString(", ")
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
 * 支持多语种（简体中文、繁体中文、日文及英文）本地预设城市快速检索。
 */
object CitySearchService {

    /**
     * 本地预设城市模糊匹配（支持语种感知：简体中文、繁体中文、日文及英文）。
     */
    fun searchLocal(query: String, locale: Locale = Locale.getDefault()): List<CitySearchResult> {
        val trimmed = query.trim()
        val lowerQuery = trimmed.lowercase()

        val matchingPresetList = if (trimmed.isEmpty()) {
            PRESET_CITIES
        } else {
            PRESET_CITIES.filter { city ->
                city.getLocalizedCityName(locale).lowercase().contains(lowerQuery) ||
                        city.englishName.lowercase().contains(lowerQuery) ||
                        city.nameZhCN.lowercase().contains(lowerQuery) ||
                        city.nameZhTW.lowercase().contains(lowerQuery) ||
                        city.nameJa.lowercase().contains(lowerQuery) ||
                        city.zoneId.id.lowercase().contains(lowerQuery)
            }
        }

        return matchingPresetList.map { city ->
            CitySearchResult(
                cityName = city.getLocalizedCityName(locale),
                country = city.getLocalizedCountry(locale),
                zoneId = city.zoneId,
                isOnline = false
            )
        }.distinctBy { "${it.displayLocationName}_${it.zoneId.id}" }
    }

    /**
     * 在线搜索全球城市及关联的 IANA 时区（Open-Meteo 免费 API，支持根据语种偏好请求对应语种名称）。
     */
    suspend fun searchOnline(query: String, locale: Locale = Locale.getDefault()): List<CitySearchResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val language = when (locale.language) {
                "zh" -> "zh"
                "ja" -> "ja"
                else -> "en"
            }
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
