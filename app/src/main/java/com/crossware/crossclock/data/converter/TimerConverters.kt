package com.crossware.crossclock.data.converter

import androidx.room.TypeConverter
import com.crossware.crossclock.data.timer.TimerStep
import com.crossware.crossclock.data.timer.TimerType
import org.json.JSONArray
import org.json.JSONObject

class TimerConverters {

    @TypeConverter
    fun timerTypeToString(timerType: TimerType): String {
        return timerType.name
    }

    @TypeConverter
    fun stringToTimerType(string: String): TimerType {
        return try {
            TimerType.valueOf(string)
        } catch (e: Exception) {
            TimerType.SINGLE
        }
    }

    @TypeConverter
    fun timerStepsToString(steps: List<TimerStep>): String {
        val jsonArray = JSONArray()
        for (step in steps) {
            val jsonObject = JSONObject().apply {
                put("id", step.id)
                put("name", step.name)
                put("durationSeconds", step.durationSeconds)
                put("colorHex", step.colorHex)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun stringToTimerSteps(string: String): List<TimerStep> {
        if (string.isBlank()) return emptyList()
        val list = mutableListOf<TimerStep>()
        try {
            val jsonArray = JSONArray(string)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                val id = item.optString("id", "")
                val name = item.optString("name", "")
                val durationSeconds = item.optLong("durationSeconds", 0)
                val colorHex = item.optString("colorHex", "#3F51B5")
                list.add(TimerStep(id, name, durationSeconds, colorHex))
            }
        } catch (e: Exception) {
            // ignore
        }
        return list
    }
}
