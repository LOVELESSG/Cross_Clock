package com.example.crossclock.data.worldclock

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.crossclock.data.ZoneIdConverter
import java.time.ZoneId

@Entity
@TypeConverters(ZoneIdConverter::class)
data class WorldClock (
    val city: String,
    val cityTimeZoneId: ZoneId,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0
)