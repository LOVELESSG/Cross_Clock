package com.example.crossclock.data.worldclock

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.crossclock.data.converter.ZoneIdConverter
import java.time.ZoneId

@Entity
data class WorldClock (
    val city: String,
    val cityTimeZoneId: ZoneId,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0
)