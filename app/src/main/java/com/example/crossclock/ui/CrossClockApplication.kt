package com.example.crossclock.ui

import android.app.Application
import com.example.crossclock.Graph

class CrossClockApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.provide(this)
    }
}