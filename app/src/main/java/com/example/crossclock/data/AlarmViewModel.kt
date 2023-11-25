package com.example.crossclock.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crossclock.Graph
import com.example.crossclock.data.alarm.Alarm
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AlarmViewModel(
    private val repository: Repository = Graph.repository
): ViewModel() {
    var state by mutableStateOf(AlarmState())
        private set

    init {
        getAllAlarm()
    }
    private fun getAllAlarm(){
        viewModelScope.launch {
            repository.allAlarm.collectLatest {
                state = state.copy(
                    items = it
                )
            }
        }
    }

    fun addAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.insertAlarm(alarm)
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
        }
    }

    fun updateAlarmStatus(alarm: Alarm) {
        viewModelScope.launch {
            repository.updateAlarmStatus(alarm)
        }
    }
}

data class AlarmState(
    val items: List<Alarm> = emptyList()
)