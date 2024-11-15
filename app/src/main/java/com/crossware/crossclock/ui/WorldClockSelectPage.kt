package com.crossware.crossclock.ui

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
import com.crossware.crossclock.data.worldclock.WorldClock
import com.crossware.crossclock.data.WorldClockState
import kotlinx.coroutines.flow.Flow
import java.time.format.TextStyle
import java.util.Locale


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
                    headlineContent = { Text(
                        text = timezoneInfo.city
                                +" -- "
                                +timezoneInfo
                                    .cityTimeZoneId
                                    .getDisplayName(TextStyle.FULL_STANDALONE, Locale.ENGLISH)
                    ) },
                    trailingContent = {
                        Checkbox(
                            checked = existItem,
                            onCheckedChange = {
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