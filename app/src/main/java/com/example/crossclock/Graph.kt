package com.example.crossclock

import android.content.Context
import com.example.crossclock.data.AppDatabase
import com.example.crossclock.data.Repository

object Graph {
    private lateinit var db: AppDatabase

    val repository by lazy {
        Repository(
            worldClockDao = db.worldClockDao()
        )
    }

    fun provide(context: Context){
        db = AppDatabase.getDatabase(context)
    }
}