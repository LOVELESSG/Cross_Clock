package com.example.crossclock.data

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class WorldClockViewModel @Inject constructor(private val worldClockDao: WorldClockDao): ViewModel() {
    val allAddedCities: Flow<List<WorldClock>> = worldClockDao.loadAllWorldClock()
}