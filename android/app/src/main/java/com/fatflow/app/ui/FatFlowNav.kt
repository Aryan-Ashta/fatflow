package com.fatflow.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fatflow.app.di.AppContainer
import com.fatflow.app.ui.checkin.CheckInScreen
import com.fatflow.app.ui.food.BarcodeScreen
import com.fatflow.app.ui.food.FoodSearchScreen
import com.fatflow.app.ui.onboarding.OnboardingScreen
import com.fatflow.app.ui.settings.SettingsScreen
import com.fatflow.app.ui.today.TodayScreen
import com.fatflow.app.ui.today.TodayViewModel
import com.fatflow.app.ui.today.TodayViewModelFactory
import com.fatflow.app.ui.trajectory.TrajectoryScreen
import com.fatflow.app.ui.trajectory.TrajectoryViewModel
import com.fatflow.app.ui.trajectory.TrajectoryViewModelFactory
import com.fatflow.app.ui.week.WeekScreen
import com.fatflow.app.ui.week.WeekViewModel
import com.fatflow.app.ui.week.WeekViewModelFactory

@Composable
fun FatFlowNav(container: AppContainer, deepLinkBarcode: Boolean = false) {
    val nav = rememberNavController()
    LaunchedEffect(deepLinkBarcode) {
        if (deepLinkBarcode) nav.navigate("barcode")
    }
    val profile by container.profileStore.profile.collectAsState(initial = null)
    val start = if (profile?.onboardingComplete == true) "today" else "onboarding"
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val showBottomBar = backStack?.destination?.route in listOf("today", "week", "trajectory")
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    listOf("today" to "Today", "week" to "Week", "trajectory" to "Trajectory").forEach { (route, label) ->
                        NavigationBarItem(
                            selected = backStack?.destination?.route == route,
                            onClick = { nav.navigate(route) { launchSingleTop = true } },
                            label = { Text(label) },
                            icon = { Text("•") },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.padding(padding),
        ) {
            composable("onboarding") {
                OnboardingScreen(container, snackbar) {
                    nav.navigate("today") { popUpTo("onboarding") { inclusive = true } }
                }
            }
            composable("today") {
                val vm: TodayViewModel = viewModel(factory = TodayViewModelFactory(container))
                TodayScreen(
                    vm,
                    snackbar,
                    onSettings = { nav.navigate("settings") },
                    onSearch = { nav.navigate("food_search") },
                    onScan = { nav.navigate("barcode") },
                    onCheckIn = { nav.navigate("check_in") },
                )
            }
            composable("week") {
                val vm: WeekViewModel = viewModel(factory = WeekViewModelFactory(container))
                WeekScreen(vm)
            }
            composable("trajectory") {
                val vm: TrajectoryViewModel = viewModel(factory = TrajectoryViewModelFactory(container))
                TrajectoryScreen(vm) { nav.navigate("check_in") }
            }
            composable("check_in") {
                CheckInScreen(container) { nav.popBackStack() }
            }
            composable("food_search") {
                FoodSearchScreen(container) { nav.popBackStack() }
            }
            composable("barcode") {
                BarcodeScreen(container) { nav.popBackStack() }
            }
            composable("settings") {
                SettingsScreen(container) { nav.popBackStack() }
            }
        }
    }
}