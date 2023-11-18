package com.example.crossclock.data.alarm

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Entity
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val time: LocalDateTime,
    val message: String,
    val timeZone: ZoneId,
    //val date: String
)
