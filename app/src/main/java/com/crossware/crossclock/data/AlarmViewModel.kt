package com.crossware.crossclock.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossware.crossclock.data.alarm.Alarm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val repository: Repository
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