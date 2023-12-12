package com.crossware.crossclock.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossware.crossclock.Graph
import com.crossware.crossclock.data.worldclock.WorldClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WorldClockViewModel(
    private val repository: Repository = Graph.repository
): ViewModel(){
    var state by mutableStateOf(WorldClockState())
        private set

    init {
        getAllWorldClock()
    }
    private fun getAllWorldClock(){
        viewModelScope.launch {
            repository.allWorldClock.collectLatest {
                state = state.copy(
                    items = it
                )
            }
        }
    }

    fun addWorldClock(worldClock: WorldClock){
        viewModelScope.launch {
            repository.insertWorldClock(worldClock)
        }
    }

    fun deleteWorldClock(worldClock: WorldClock){
        viewModelScope.launch {
            repository.deleteWorldClock(worldClock)
        }
    }

    fun deleteWorldClockByName(city: String){
        viewModelScope.launch{
            repository.deleteWorldClockByName(city)
        }
    }

    fun existWorldClock(city: String): Flow<Boolean> {
        return repository.existsWorldClock(city)
    }
}


data class WorldClockState(
    val items: List<WorldClock> = emptyList(),
)