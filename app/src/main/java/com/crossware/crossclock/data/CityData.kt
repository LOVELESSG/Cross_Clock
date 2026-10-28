package com.crossware.crossclock.data

import com.crossware.crossclock.data.worldclock.WorldClock
import java.time.ZoneId
import java.util.Locale

/**
 * 预定义城市本地化数据模型。
 */
data class PresetCity(
    val englishName: String,
    val zoneId: ZoneId,
    val nameZhCN: String,
    val nameZhTW: String,
    val nameJa: String,
    val countryZhCN: String? = null,
    val countryZhTW: String? = null,
    val countryJa: String? = null,
    val countryEn: String? = null
) {
    /**
     * 根据 Locale 语言设置获取本地化的城市显示名称。
     */
    fun getLocalizedCityName(locale: Locale = Locale.getDefault()): String {
        val lang = locale.language
        val script = locale.script
        val country = locale.country

        return when {
            lang == "zh" -> {
                if (script.contains("Hant", ignoreCase = true) || country.equals("TW", ignoreCase = true) || country.equals("HK", ignoreCase = true)) {
                    nameZhTW
                } else {
                    nameZhCN
                }
            }
            lang == "ja" -> nameJa
            else -> englishName
        }
    }

    /**
     * 根据 Locale 语言设置获取本地化的国家名称。
     */
    fun getLocalizedCountry(locale: Locale = Locale.getDefault()): String? {
        val lang = locale.language
        val script = locale.script
        val country = locale.country

        return when {
            lang == "zh" -> {
                if (script.contains("Hant", ignoreCase = true) || country.equals("TW", ignoreCase = true) || country.equals("HK", ignoreCase = true)) {
                    countryZhTW
                } else {
                    countryZhCN
                }
            }
            lang == "ja" -> countryJa
            else -> countryEn
        }
    }

    /**
     * 获取非冗余的城市与国家显示字符串（仅用当前 App 语种展示，无冗余英文与重复国家名）。
     * 例如："檀香山, 美国" 或 "新加坡"
     */
    fun getDisplayLocationName(locale: Locale = Locale.getDefault()): String {
        val locCity = getLocalizedCityName(locale)
        val locCountry = getLocalizedCountry(locale)

        return if (!locCountry.isNullOrBlank() && !locCity.equals(locCountry, ignoreCase = true)) {
            "$locCity, $locCountry"
        } else {
            locCity
        }
    }
}

/**
 * 全球预置世界城市列表（全面支持英文、简体中文、繁体中文及日文四语种高保真本地化）。
 */
val PRESET_CITIES = listOf(
    PresetCity("Midway", ZoneId.of("Pacific/Midway"), "中途岛", "中途島", "ミッドウェー"),
    PresetCity("Honolulu", ZoneId.of("Pacific/Honolulu"), "檀香山", "檀香山", "ホノルル", "美国", "美國", "アメリカ", "USA"),
    PresetCity("Anchorage", ZoneId.of("America/Anchorage"), "安克雷奇", "安克雷奇", "アンカレッジ", "美国", "美國", "アメリカ", "USA"),
    PresetCity("Los Angeles", ZoneId.of("America/Los_Angeles"), "洛杉矶", "洛杉磯", "ロサンゼルス", "美国", "美國", "アメリカ", "USA"),
    PresetCity("San Francisco", ZoneId.of("America/Los_Angeles"), "旧金山", "舊金山", "サンフランシスコ", "美国", "美國", "アメリカ", "USA"),
    PresetCity("Tijuana", ZoneId.of("America/Tijuana"), "蒂华纳", "蒂華納", "ティフアナ", "墨西哥", "墨西哥", "メキシコ", "Mexico"),
    PresetCity("Phoenix", ZoneId.of("America/Phoenix"), "菲尼克斯", "菲尼克斯", "フェニックス", "美国", "美國", "模國", "USA"),
    PresetCity("Chihuahua", ZoneId.of("America/Chihuahua"), "奇瓦瓦", "奇瓦瓦", "チワワ", "墨西哥", "墨西哥", "メキシコ", "Mexico"),
    PresetCity("Denver", ZoneId.of("America/Denver"), "丹佛", "丹佛", "デンバー", "美国", "美國", "アメリカ", "USA"),
    PresetCity("Costa Rica", ZoneId.of("America/Costa_Rica"), "哥斯达黎加", "哥斯達黎加", "コスタリカ"),
    PresetCity("Chicago", ZoneId.of("America/Chicago"), "芝加哥", "芝加哥", "シカゴ", "美国", "美國", "アメリカ", "USA"),
    PresetCity("Mexico City", ZoneId.of("America/Mexico_City"), "墨西哥城", "墨西哥城", "メキシコシティ", "墨西哥", "墨西哥", "メキシコ", "Mexico"),
    PresetCity("Regina", ZoneId.of("America/Regina"), "里贾纳", "里賈納", "レジーナ", "加拿大", "加拿大", "カナダ", "Canada"),
    PresetCity("Bogota", ZoneId.of("America/Bogota"), "波哥大", "波哥大", "ボゴタ", "哥伦比亚", "哥倫比亞", "コロンビア", "Colombia"),
    PresetCity("New York", ZoneId.of("America/New_York"), "纽约", "紐約", "ニューヨーク", "美国", "美國", "アメリカ", "USA"),
    PresetCity("Caracas", ZoneId.of("America/Caracas"), "加拉加斯", "加拉加斯", "カラカス", "委内瑞拉", "委內瑞拉", "ベネズエラ", "Venezuela"),
    PresetCity("Barbados", ZoneId.of("America/Barbados"), "巴巴多斯", "巴巴多斯", "バルバドス"),
    PresetCity("Halifax", ZoneId.of("America/Halifax"), "哈利法克斯", "哈利法克斯", "ハリファックス", "加拿大", "加拿大", "カナダ", "Canada"),
    PresetCity("Manaus", ZoneId.of("America/Manaus"), "马瑙斯", "馬瑙斯", "マナウス", "巴西", "巴西", "ブラジル", "Brazil"),
    PresetCity("St. John's", ZoneId.of("America/St_Johns"), "圣约翰斯", "聖約翰斯", "セントジョーンズ", "加拿大", "加拿大", "カナダ", "Canada"),
    PresetCity("Santiago", ZoneId.of("America/Santiago"), "圣地亚哥", "聖地亞哥", "サンティアゴ", "智利", "智利", "チリ", "Chile"),
    PresetCity("Recife", ZoneId.of("America/Recife"), "累西腓", "累西腓", "レシフェ", "巴西", "巴西", "ブラジル", "Brazil"),
    PresetCity("Sao Paulo", ZoneId.of("America/Sao_Paulo"), "圣保罗", "聖保羅", "サンパウロ", "巴西", "巴西", "ブラジル", "Brazil"),
    PresetCity("Buenos Aires", ZoneId.of("America/Buenos_Aires"), "布宜诺斯艾利斯", "布宜諾斯艾利斯", "ブエノスアイレス", "阿根廷", "阿根廷", "アルゼンチン", "Argentina"),
    PresetCity("Nuuk", ZoneId.of("America/Godthab"), "努克", "努克", "ヌーク", "格陵兰", "格陵蘭", "グリーンランド", "Greenland"),
    PresetCity("Montevideo", ZoneId.of("America/Montevideo"), "蒙得维的亚", "蒙得維的亞", "モンテビデオ", "乌拉圭", "烏拉圭", "ウルグアイ", "Uruguay"),
    PresetCity("South Georgia", ZoneId.of("Atlantic/South_Georgia"), "南乔治亚", "南喬治亞", "サウスジョージア"),
    PresetCity("Azores", ZoneId.of("Atlantic/Azores"), "亚速尔群岛", "亞速爾群島", "アゾレス"),
    PresetCity("Cape Verde", ZoneId.of("Atlantic/Cape_Verde"), "佛得角", "佛得角", "カーボベルデ"),
    PresetCity("Casablanca", ZoneId.of("Africa/Casablanca"), "卡萨布兰卡", "卡薩布蘭卡", "カサブランカ", "摩洛哥", "摩洛哥", "モロッコ", "Morocco"),
    PresetCity("Greenwich", ZoneId.of("Etc/Greenwich"), "格林威治", "格林威治", "グリニッジ", "英国", "英國", "イギリス", "UK"),
    PresetCity("Amsterdam", ZoneId.of("Europe/Amsterdam"), "阿姆斯特丹", "阿姆斯特丹", "アムステルダム", "荷兰", "荷蘭", "オランダ", "Netherlands"),
    PresetCity("Belgrade", ZoneId.of("Europe/Belgrade"), "贝尔格莱德", "貝爾格萊德", "ベオグラード", "塞尔维亚", "塞爾維亞", "セルビア", "Serbia"),
    PresetCity("Brussels", ZoneId.of("Europe/Brussels"), "布鲁塞尔", "布魯塞爾", "ブリュッセル", "比利时", "比利時", "ベルギー", "Belgium"),
    PresetCity("Madrid", ZoneId.of("Europe/Madrid"), "马德里", "馬德里", "マドリード", "西班牙", "西班牙", "スペイン", "Spain"),
    PresetCity("Sarajevo", ZoneId.of("Europe/Sarajevo"), "萨拉热窝", "薩拉熱窩", "サラエヴォ", "波黑", "波黑", "ボスニア", "Bosnia"),
    PresetCity("Berlin", ZoneId.of("Europe/Berlin"), "柏林", "柏林", "ベルリン", "德国", "德國", "ドイツ", "Germany"),
    PresetCity("Zurich", ZoneId.of("Europe/Zurich"), "苏黎世", "蘇黎世", "チューリッヒ", "瑞士", "瑞士", "スイス", "Switzerland"),
    PresetCity("Paris", ZoneId.of("Europe/Paris"), "巴黎", "巴黎", "パリ", "法国", "法國", "フランス", "France"),
    PresetCity("Rome", ZoneId.of("Europe/Rome"), "罗马", "羅馬", "ローマ", "意大利", "義大利", "イタリア", "Italy"),
    PresetCity("London", ZoneId.of("Europe/London"), "伦敦", "倫敦", "ロンドン", "英国", "英國", "イギリス", "UK"),
    PresetCity("Brazzaville", ZoneId.of("Africa/Brazzaville"), "布拉柴维尔", "布拉柴維爾", "ブラザヴィル"),
    PresetCity("Windhoek", ZoneId.of("Africa/Windhoek"), "温得和克", "溫得和克", "ウィンドフック"),
    PresetCity("Amman", ZoneId.of("Asia/Amman"), "安曼", "安曼", "アンマン", "约旦", "約旦", "ヨルダン", "Jordan"),
    PresetCity("Athens", ZoneId.of("Europe/Athens"), "雅典", "雅典", "アテネ", "希腊", "希臘", "ギリシャ", "Greece"),
    PresetCity("Istanbul", ZoneId.of("Europe/Istanbul"), "伊斯坦布尔", "伊斯坦堡", "イ斯坦ブール", "土耳其", "土耳其", "トルコ", "Turkey"),
    PresetCity("Beirut", ZoneId.of("Asia/Beirut"), "贝鲁特", "貝魯特", "ベイルート", "黎巴嫩", "黎巴嫩", "レバノン", "Lebanon"),
    PresetCity("Cairo", ZoneId.of("Africa/Cairo"), "开罗", "開羅", "カイロ", "埃及", "埃及", "エジプト", "Egypt"),
    PresetCity("Helsinki", ZoneId.of("Europe/Helsinki"), "赫尔辛基", "赫爾辛基", "ヘルシンキ", "芬兰", "芬蘭", "フィンランド", "Finland"),
    PresetCity("Jerusalem", ZoneId.of("Asia/Jerusalem"), "耶路撒冷", "耶路撒冷", "エルサレム"),
    PresetCity("Harare", ZoneId.of("Africa/Harare"), "哈拉雷", "哈拉雷", "ハラレ"),
    PresetCity("Minsk", ZoneId.of("Europe/Minsk"), "明斯克", "明斯克", "ミンスク"),
    PresetCity("Baghdad", ZoneId.of("Asia/Baghdad"), "巴格达", "巴格達", "バグダッド", "伊拉克", "伊拉克", "イラク", "Iraq"),
    PresetCity("Moscow", ZoneId.of("Europe/Moscow"), "莫斯科", "莫斯科", "モスクワ", "俄罗斯", "俄羅斯", "ロシア", "Russia"),
    PresetCity("Kuwait", ZoneId.of("Asia/Kuwait"), "科威特城", "科威特城", "クウェート"),
    PresetCity("Nairobi", ZoneId.of("Africa/Nairobi"), "内罗毕", "奈洛比", "ナイロビ"),
    PresetCity("Tehran", ZoneId.of("Asia/Tehran"), "德黑兰", "德黑蘭", "テヘラン", "伊朗", "伊朗", "イラン", "Iran"),
    PresetCity("Dubai", ZoneId.of("Asia/Dubai"), "迪拜", "杜拜", "ドバイ", "阿联酋", "阿聯酋", "アラブ首長国連邦", "UAE"),
    PresetCity("Kabul", ZoneId.of("Asia/Kabul"), "喀布尔", "喀布爾", "カブール"),
    PresetCity("Karachi", ZoneId.of("Asia/Karachi"), "卡拉奇", "喀拉蚩", "カラチ"),
    PresetCity("Yekaterinburg", ZoneId.of("Asia/Yekaterinburg"), "叶卡捷琳堡", "葉卡捷琳堡", "エカテリンブルク"),
    PresetCity("Kolkata", ZoneId.of("Asia/Kolkata"), "加尔各答", "加爾各答", "コルカタ", "印度", "印度", "インド", "India"),
    PresetCity("Colombo", ZoneId.of("Asia/Colombo"), "科伦坡", "科倫坡", "コロンボ"),
    PresetCity("Kathmandu", ZoneId.of("Asia/Kathmandu"), "加德满都", "加德滿都", "カトマンズ"),
    PresetCity("Almaty", ZoneId.of("Asia/Almaty"), "阿拉木图", "阿拉木圖", "アルマトイ"),
    PresetCity("Yangon", ZoneId.of("Asia/Rangoon"), "仰光", "仰光", "ヤンゴン"),
    PresetCity("Bangkok", ZoneId.of("Asia/Bangkok"), "曼谷", "曼谷", "バンコク", "泰国", "泰國", "タイ", "Thailand"),
    PresetCity("Jakarta", ZoneId.of("Asia/Jakarta"), "雅加达", "雅加達", "ジャカルタ", "印度尼西亚", "印尼", "インドネシア", "Indonesia"),
    PresetCity("Beijing", ZoneId.of("Asia/Shanghai"), "北京", "北京", "北京", "中国", "中國", "中国", "China"),
    PresetCity("Shanghai", ZoneId.of("Asia/Shanghai"), "上海", "上海", "上海", "中国", "中國", "中国", "China"),
    PresetCity("Hong Kong", ZoneId.of("Asia/Hong_Kong"), "香港", "香港", "香港", "中国香港", "香港", "香港", "Hong Kong"),
    PresetCity("Irkutsk", ZoneId.of("Asia/Irkutsk"), "伊尔库茨克", "伊爾庫茨克", "イルクーツク"),
    PresetCity("Kuala Lumpur", ZoneId.of("Asia/Kuala_Lumpur"), "吉隆坡", "吉隆坡", "クアラルンプール", "马来西亚", "馬來西亞", "マレーシア", "Malaysia"),
    PresetCity("Singapore", ZoneId.of("Asia/Singapore"), "新加坡", "新加坡", "シンガポール", "新加坡", "新加坡", "シンガポール", "Singapore"),
    PresetCity("Perth", ZoneId.of("Australia/Perth"), "珀斯", "珀斯", "パース", "澳大利亚", "澳大利亞", "オーストラリア", "Australia"),
    PresetCity("Taipei", ZoneId.of("Asia/Taipei"), "台北", "台北", "台北", "中国台湾", "台灣", "台湾", "Taiwan"),
    PresetCity("Seoul", ZoneId.of("Asia/Seoul"), "首尔", "首爾", "ソウル", "韩国", "韓國", "韓国", "South Korea"),
    PresetCity("Tokyo", ZoneId.of("Asia/Tokyo"), "东京", "東京", "東京", "日本", "日本", "日本", "Japan"),
    PresetCity("Darwin", ZoneId.of("Australia/Darwin"), "达尔文", "達爾文", "ダーウィン", "澳大利亚", "澳大利亞", "オーストラリア", "Australia"),
    PresetCity("Brisbane", ZoneId.of("Australia/Brisbane"), "布里斯班", "布里斯班", "ブリスベン", "澳大利亚", "澳大利亞", "オーストラリア", "Australia"),
    PresetCity("Vladivostok", ZoneId.of("Asia/Vladivostok"), "符拉迪沃斯托克", "海參崴", "ウラジオストク", "俄罗斯", "俄羅斯", "ロシア", "Russia"),
    PresetCity("Guam", ZoneId.of("Pacific/Guam"), "关岛", "關島", "グアム"),
    PresetCity("Adelaide", ZoneId.of("Australia/Adelaide"), "阿德莱德", "阿德萊德", "アデレード", "澳大利亚", "澳大利亞", "オーストラリア", "Australia"),
    PresetCity("Sydney", ZoneId.of("Australia/Sydney"), "悉尼", "雪梨", "シドニー", "澳大利亚", "澳大利亞", "オーストラリア", "Australia"),
    PresetCity("Auckland", ZoneId.of("Pacific/Auckland"), "奥克兰", "奧克蘭", "オークランド", "新西兰", "紐西蘭", "ニュージーランド", "New Zealand")
)

/**
 * 保持兼容性的 ALL_CITIES 列表。
 */
val ALL_CITIES: List<WorldClock> = PRESET_CITIES.map {
    WorldClock(city = it.getDisplayLocationName(), country = it.getLocalizedCountry(), cityTimeZoneId = it.zoneId)
}
