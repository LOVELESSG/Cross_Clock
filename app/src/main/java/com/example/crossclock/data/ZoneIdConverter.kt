package com.example.crossclock.data

import androidx.room.TypeConverter
import java.time.ZoneId

class ZoneIdConverter {

    @TypeConverter
    fun zoneIdToString(zoneId: ZoneId): String {
        return zoneId.toString()
    }

    @TypeConverter
    fun stringToZoneId(string: String): ZoneId {
        return ZoneId.of(string)
    }
}