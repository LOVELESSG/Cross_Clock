package com.crossware.crossclock.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.crossware.crossclock.data.DRAWER_ITEMS
import com.crossware.crossclock.data.WorldClockViewModel
import com.crossware.crossclock.data.worldclock.WorldClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossClockApp(
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var openBottomSheet by rememberSaveable {
        mutableStateOf(false)
    }
    val selectedItem = remember {
        mutableStateOf(DRAWER_ITEMS[0])
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val worldClockViewModel = viewModel(modelClass = WorldClockViewModel::class.java)
    val worldClockState = worldClockViewModel.state
    val homepageList = worldClockState.items

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .requiredWidth(200.dp)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "CrossClock",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(0.dp, 4.dp, 0.dp, 4.dp))
                DRAWER_ITEMS.forEach {item ->
                    NavigationDrawerItem(
                        icon = { Icon(painter = painterResource(id = item.icon), contentDescription = null) },
                        label = {
                            Text(
                                text = (item.name),
                                modifier = Modifier.padding(8.dp))
                        },
                        selected = item == selectedItem.value,
                        onClick = {
                            scope.launch { drawerState.close() }
                            selectedItem.value = item
                            navController.navigate(route = item.route)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(text = "World Clock")
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Function List"
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { openBottomSheet = !openBottomSheet }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add a new city")
                }
            }
        ) { padding ->
            WorldClockContent(homepageList, padding)
            if (openBottomSheet) {
                WorldClockSelectPage(
                    worldClockViewModel.state,
                    changeStatus = { openBottomSheet = !openBottomSheet },
                    addWorldClock = worldClockViewModel::addWorldClock,
                    removeWorldClockByName = worldClockViewModel::deleteWorldClockByName,
                    isChecked = worldClockViewModel::existWorldClock
                )
            }
        }
    }
    BackHandler(
        enabled = drawerState.isOpen
    ) {
        scope.launch {
            drawerState.close()
        }
    }
}

@Composable
fun WorldClockContent(
    homepageList: List<WorldClock>,
    padding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.TopStart
    ) {
        LazyColumn{
            itemsIndexed(homepageList) { _, item ->
                var currentTime by remember {
                    mutableStateOf(LocalTime.now(item.cityTimeZoneId)
                        .format(DateTimeFormatter.ofPattern("HH:mm")))
                }
                LaunchedEffect(key1 = true) {
                    while (true) {
                        currentTime = LocalTime.now(item.cityTimeZoneId)
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                        delay(1000)
                    }
                }
                ListItem(
                    headlineContent = { Text(text = item.city, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(
                        text = LocalDate.now(item.cityTimeZoneId)
                            .format(DateTimeFormatter.ISO_DATE)) },
                    trailingContent = { Text(
                        text = currentTime,
                        fontSize = 45.sp
                    )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(16.dp, 4.dp, 16.dp, 8.dp))
            }
        }
    }
}


val ALL_CITIES = arrayListOf(
    WorldClock("Midway", ZoneId.of("Pacific/Midway")),
    WorldClock("Honolulu", ZoneId.of("Pacific/Honolulu")),
    WorldClock("Anchorage", ZoneId.of("America/Anchorage")),
    WorldClock("Los Angeles", ZoneId.of("America/Los_Angeles")),
    WorldClock( "Tijuana", ZoneId.of("America/Tijuana")),
    WorldClock("Phoenix", ZoneId.of("America/Phoenix")),
    WorldClock("Chihuahua", ZoneId.of("America/Chihuahua")),
    WorldClock("Denver", ZoneId.of("America/Denver")),
    WorldClock("Costa Rica", ZoneId.of("America/Costa_Rica")),
    WorldClock("Chicago", ZoneId.of("America/Chicago")),
    WorldClock("Mexico City", ZoneId.of("America/Mexico_City")),
    WorldClock("Regina", ZoneId.of("America/Regina")),
    WorldClock("Bogota", ZoneId.of("America/Bogota")),
    WorldClock( "New York", ZoneId.of("America/New_York")),
    WorldClock("Caracas", ZoneId.of("America/Caracas")),
    WorldClock("Barbados", ZoneId.of("America/Barbados")),
    WorldClock("Halifax", ZoneId.of("America/Halifax")),
    WorldClock("Manaus", ZoneId.of("America/Manaus")),
    WorldClock("St. John's", ZoneId.of("America/St_Johns")),
    WorldClock("Santiago", ZoneId.of("America/Santiago")),
    WorldClock("Recife", ZoneId.of("America/Recife")),
    WorldClock( "Sao Paulo", ZoneId.of("America/Sao_Paulo")),
    WorldClock("Buenos Aires", ZoneId.of("America/Buenos_Aires")),
    WorldClock( "Nuuk", ZoneId.of("America/Godthab")),
    WorldClock("Montevideo", ZoneId.of("America/Montevideo")),
    WorldClock("South Georgia", ZoneId.of("Atlantic/South_Georgia")),
    WorldClock("Azores", ZoneId.of("Atlantic/Azores")),
    WorldClock("Cape Verde", ZoneId.of("Atlantic/Cape_Verde")),
    WorldClock( "Casablanca", ZoneId.of("Africa/Casablanca")),
    WorldClock("Greenwich Mean Time", ZoneId.of("Etc/Greenwich")),
    WorldClock( "Amsterdam", ZoneId.of("Europe/Amsterdam")),
    WorldClock("Belgrade", ZoneId.of("Europe/Belgrade")),
    WorldClock("Brussels", ZoneId.of("Europe/Brussels")),
    WorldClock( "Madrid", ZoneId.of("Europe/Madrid")),
    WorldClock("Sarajevo", ZoneId.of("Europe/Sarajevo")),
    WorldClock("Brazzaville", ZoneId.of("Africa/Brazzaville")),
    WorldClock( "Windhoek", ZoneId.of("Africa/Windhoek")),
    WorldClock( "Amman", ZoneId.of("Asia/Amman")),
    WorldClock("Athens", ZoneId.of("Europe/Athens")),
    WorldClock( "Istanbul", ZoneId.of("Europe/Istanbul")),
    WorldClock( "Beirut", ZoneId.of("Asia/Beirut")),
    WorldClock("Cairo", ZoneId.of("Africa/Cairo")),
    WorldClock( "Helsinki", ZoneId.of("Europe/Helsinki")),
    WorldClock("Jerusalem", ZoneId.of("Asia/Jerusalem")),
    WorldClock("Harare", ZoneId.of("Africa/Harare")),
    WorldClock("Minsk", ZoneId.of("Europe/Minsk")),
    WorldClock( "Baghdad", ZoneId.of("Asia/Baghdad")),
    WorldClock("Moscow", ZoneId.of("Europe/Moscow")),
    WorldClock("Kuwait", ZoneId.of("Asia/Kuwait")),
    WorldClock("Nairobi", ZoneId.of("Africa/Nairobi")),
    WorldClock( "Tehran", ZoneId.of("Asia/Tehran")),
    WorldClock( "Baku", ZoneId.of("Asia/Baku")),
    WorldClock( "Tbilisi", ZoneId.of("Asia/Tbilisi")),
    WorldClock( "Yerevan", ZoneId.of("Asia/Yerevan")),
    WorldClock( "Dubai", ZoneId.of("Asia/Dubai")),
    WorldClock( "Kabul", ZoneId.of("Asia/Kabul")),
    WorldClock( "Karachi", ZoneId.of("Asia/Karachi")),
    WorldClock("Oral", ZoneId.of("Asia/Oral")),
    WorldClock("Yekaterinburg", ZoneId.of("Asia/Yekaterinburg")),
    WorldClock("Kolkata", ZoneId.of("Asia/Kolkata")),
    WorldClock( "Colombo", ZoneId.of("Asia/Colombo")),
    WorldClock( "Kathmandu", ZoneId.of("Asia/Kathmandu")),
    WorldClock("Almaty", ZoneId.of("Asia/Almaty")),
    WorldClock( "Rangoon", ZoneId.of("Asia/Rangoon")),
    WorldClock( "Krasnoyarsk", ZoneId.of("Asia/Krasnoyarsk")),
    WorldClock("Bangkok", ZoneId.of("Asia/Bangkok")),
    WorldClock("Jakarta", ZoneId.of("Asia/Jakarta")),
    WorldClock("Shanghai", ZoneId.of("Asia/Shanghai")),
    WorldClock("Hong Kong", ZoneId.of("Asia/Hong_Kong")),
    WorldClock( "Irkutsk", ZoneId.of("Asia/Irkutsk")),
    WorldClock("Kuala Lumpur", ZoneId.of("Asia/Kuala_Lumpur")),
    WorldClock("Perth", ZoneId.of("Australia/Perth")),
    WorldClock( "Taipei", ZoneId.of("Asia/Taipei")),
    WorldClock( "Seoul", ZoneId.of("Asia/Seoul")),
    WorldClock( "Tokyo", ZoneId.of("Asia/Tokyo")),
    WorldClock("Yakutsk", ZoneId.of("Asia/Yakutsk")),
    WorldClock("Darwin", ZoneId.of("Australia/Darwin")),
    WorldClock("Brisbane", ZoneId.of("Australia/Brisbane")),
    WorldClock( "Vladivostok", ZoneId.of("Asia/Vladivostok")),
    WorldClock( "Guam", ZoneId.of("Pacific/Guam")),
    WorldClock( "Magadan", ZoneId.of("Asia/Magadan")),
    WorldClock( "Adelaide", ZoneId.of("Australia/Adelaide")),
    WorldClock("Hobart", ZoneId.of("Australia/Hobart")),
    WorldClock("Sydney", ZoneId.of("Australia/Sydney")),
    WorldClock("Noumea", ZoneId.of("Pacific/Noumea")),
    WorldClock( "Majuro", ZoneId.of("Pacific/Majuro")),
    WorldClock("Fiji", ZoneId.of("Pacific/Fiji")),
    WorldClock("Auckland", ZoneId.of("Pacific/Auckland")),
    WorldClock("Tongatapu", ZoneId.of("Pacific/Tongatapu"))
)
