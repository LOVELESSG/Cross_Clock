package com.crossware.crossclock

import android.content.Context
import com.crossware.crossclock.data.AppDatabase
import com.crossware.crossclock.data.Repository

object Graph {
    private lateinit var db: AppDatabase

    val repository by lazy {
        Repository(
            worldClockDao = db.worldClockDao(),
            alarmDao = db.alarmDao()
        )
    }

    fun provide(context: Context){
        db = AppDatabase.getDatabase(context)
    }
}