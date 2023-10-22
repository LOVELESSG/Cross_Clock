package com.example.crossclock.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.crossclock.data.WorldClock
import com.example.crossclock.data.WorldClockViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldClockSelectPage(viewModel: WorldClockViewModel = ,changeStatus: () -> Unit){
//Before change 1
//fun WorldClockSelectPage(homepageList: MutableList<WorldClock>, changeStatus: () -> Unit){
    ModalBottomSheet(
        onDismissRequest = { changeStatus() }
    ) {
        LazyColumn{
            items(ALL_CITIES) { timezoneInfo ->
                ListItem(
                    headlineContent = { Text(text = timezoneInfo.city) },
                    trailingContent = {
                        Checkbox(
                            checked = homepageList.contains(timezoneInfo),
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    homepageList.add(timezoneInfo)
                                } else {
                                    homepageList.remove(timezoneInfo)
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}