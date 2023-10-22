package com.example.crossclock.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.time.ZoneId

@Entity
@TypeConverters(ZoneIdConverter::class)
data class WorldClock (
    val city: String,
    val cityTimeZoneId: ZoneId,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0
)