package com.fatflow.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fatflow.app.di.AppContainer
import java.time.LocalDate
import kotlin.math.ceil
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer, onBack: () -> Unit) {
    val profile by container.profileStore.profile.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    Column(Modifier.padding(16.dp)) {
        Text("Settings")
        profile?.let { p ->
            var rate by remember(p) { mutableFloatStateOf(p.releaseRate.toFloat()) }
            var goal by remember(p) { mutableFloatStateOf(p.goalWeightLbs) }
            var usda by remember(p) { mutableStateOf(p.usdaApiKey) }
            var notifications by remember(p) { mutableStateOf(p.notificationsEnabled) }
            Slider(value = rate, onValueChange = { rate = it }, valueRange = 22f..31f, steps = 8)
            OutlinedTextField(goal.toString(), { goal = it.toFloatOrNull() ?: goal }, label = { Text("Goal weight") })
            OutlinedTextField(usda, { usda = it }, label = { Text("USDA API key") })
            androidx.compose.foundation.layout.Row {
                Text("Notifications")
                Switch(notifications, { notifications = it })
            }
            Button(onClick = { /* Health Connect */ }) { Text("Connect Health Connect") }
            Button(
                onClick = {
                    scope.launch {
                        container.profileStore.updateReleaseRate(rate.toInt())
                        container.profileStore.updateGoal(goal)
                        container.profileStore.updateUsdaKey(usda)
                        container.profileStore.updateNotifications(notifications)
                        container.recomputer.recompute(LocalDate.now())
                    }
                },
            ) { Text("Save") }
        }
        Button(onClick = onBack) { Text("Back") }
    }
}