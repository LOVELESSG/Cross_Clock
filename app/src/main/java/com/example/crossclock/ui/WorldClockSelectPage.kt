package com.example.crossclock.ui

import android.util.Log
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.example.crossclock.data.worldclock.WorldClock
import com.example.crossclock.data.WorldClockState
import kotlinx.coroutines.flow.Flow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockSelectPage(
    state: WorldClockState,
    changeStatus: () -> Unit,
    addWorldClock: (WorldClock) -> Unit,
    removeWorldClockByName: (String) -> Unit,
    isChecked: (city: String) -> Flow<Boolean>
){


    ModalBottomSheet(
        onDismissRequest = { changeStatus() }
    ) {

        LazyColumn{
            items(ALL_CITIES) { timezoneInfo ->
                val existItem = isChecked(timezoneInfo.city).collectAsState(initial = false).value

                ListItem(
                    headlineContent = { Text(text = timezoneInfo.city) },
                    trailingContent = {
                        Checkbox(
                            checked = existItem,
                            onCheckedChange = {
                                Log.d("initial checked", state.items.contains(timezoneInfo).toString())
                                if (it) {
                                    addWorldClock(timezoneInfo)
                                } else{ removeWorldClockByName(timezoneInfo.city)}
                            }
                        )
                    }
                )
            }
        }
    }
}