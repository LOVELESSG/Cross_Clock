package com.example.crossclock.data.converter

import androidx.room.TypeConverter
import java.time.LocalDateTime

class LocalDateTimeConverter {
    @TypeConverter
    fun localDateTimeToString(localDateTime: LocalDateTime): String {
        return localDateTime.toString()
    }

    @TypeConverter
    fun stringToLocalDateTime(string: String): LocalDateTime {
        return LocalDateTime.parse(string)
    }
}