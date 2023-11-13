package com.example.crossclock.data.alarm

import androidx.room.Entity
import java.time.LocalTime
import java.time.ZoneId

@Entity
data class Alarm(
    val time: LocalTime,
    val message: String,
    val timeZone: ZoneId,
    val date: String
)
