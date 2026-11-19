package com.fatflow.app.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fatflow.app.FatFlowApp
import com.fatflow.app.di.AppContainer
import com.fatflow.app.domain.MetabolicEngine
import java.time.LocalDate
import kotlin.math.ceil
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    container: AppContainer,
    snackbar: SnackbarHostState,
    onDone: () -> Unit,
) {
    var weight by remember { mutableFloatStateOf(210f) }
    var bf by remember { mutableFloatStateOf(31.7f) }
    var goal by remember { mutableFloatStateOf(165f) }
    var rate by remember { mutableFloatStateOf(25f) }
    var usda by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val appContext = LocalContext.current.applicationContext
    Column(Modifier.padding(16.dp)) {
        Text("FatFlow", style = MaterialTheme.typography.headlineSmall)
        Text("Weight (lb)")
        Slider(value = weight, onValueChange = { weight = it }, valueRange = 80f..500f)
        Text("%.1f".format(weight))
        Text("Body fat %")
        Slider(value = bf, onValueChange = { bf = it }, valueRange = 3f..60f)
        Text("%.1f".format(bf))
        OutlinedTextField(value = goal.toString(), onValueChange = { goal = it.toFloatOrNull() ?: goal }, label = { Text("Goal weight") })
        Text("Release rate")
        Slider(value = rate, onValueChange = { rate = it }, valueRange = 22f..31f, steps = 8)
        OutlinedTextField(usda, { usda = it }, label = { Text("USDA API key (optional)") })
        Button(onClick = { /* Health Connect optional */ }) { Text("Connect Health Connect") }
        Button(
            onClick = {
                scope.launch {
                    val w = weight.toDouble()
                    val b = bf.toDouble()
                    val lean = MetabolicEngine.leanMassLbs(w, b)
                    var minGoal = ceil(lean / 0.87).toFloat()
                    if (goal < minGoal) {
                        snackbar.showSnackbar("Goal raised to $minGoal lb so body fat stays at least 13%.")
                        goal = minGoal
                    } else {
                        minGoal = goal
                    }
                    val fatMass = MetabolicEngine.fatMassLbs(w, b)
                    container.profileStore.saveOnboardingProfile(
                        weight, bf, fatMass.toFloat(), minGoal, rate.toInt(), usda,
                        LocalDate.now(),
                    )
                    container.bodyMetrics.record(LocalDate.now(), w, b)
                    container.recomputer.recompute(LocalDate.now())
                    (appContext as? FatFlowApp)?.enqueueMorningWork()
                    onDone()
                }
            },
        ) { Text("Save") }
    }
}