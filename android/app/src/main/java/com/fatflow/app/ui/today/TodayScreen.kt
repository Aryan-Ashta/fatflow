package com.fatflow.app.ui.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.fatflow.app.domain.GaugeStatus
import kotlinx.coroutines.launch

@Composable
fun TodayScreen(
    vm: TodayViewModel,
    snackbar: SnackbarHostState,
    onSettings: () -> Unit,
    onSearch: () -> Unit,
    onScan: () -> Unit,
    onCheckIn: () -> Unit,
) {
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    Column(Modifier.padding(16.dp)) {
        if (state.showEmptyTarget) {
            Text("Targets update each morning.")
            TextButton(onClick = { vm.recalculate() }) { Text("Recalculate") }
            return
        }
        Text(state.date.toString())
        Canvas(Modifier.height(160.dp).fillMaxWidth()) {
            val stroke = 16f
            val diameter = size.minDimension - stroke
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(diameter, diameter),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
            val color =
                when (state.gaugeStatus) {
                    GaugeStatus.ABOVE_CEILING, GaugeStatus.OVER_TARGET -> errorColor
                    else -> primaryColor
                }
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * state.gaugeFraction,
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(diameter, diameter),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text("Deficit: ${state.deficitKcal} kcal / Max Fat Supply: ${state.maxFatReleaseKcal} kcal")
        Text(state.gaugeCaption)
        FilterChip(selected = false, onClick = { vm.overrideDayType() }, label = { Text(state.dayTypeLabel) })
        MacroBar("Protein", state.proteinLogged, state.proteinTarget)
        MacroBar("Carbs", state.carbLogged, state.carbTarget)
        MacroBar("Fat", state.fatLogged, state.fatTarget)
        if (state.preWorkoutLabel.isNotBlank()) {
            Card { Text(state.preWorkoutLabel, Modifier.padding(12.dp)) }
        }
        TextButton(onClick = onScan) { Text("Scan") }
        TextButton(onClick = onSearch) { Text("Search") }
        TextButton(
            onClick = {
                scope.launch {
                    vm.repeatPreWorkout {
                        scope.launch {
                            snackbar.showSnackbar("No pre-workout meal logged yesterday.")
                        }
                    }
                }
            },
        ) { Text("Repeat pre-workout") }
        if (state.needsCheckIn) {
            TextButton(onClick = onCheckIn) { Text("Check in") }
        }
        TextButton(onClick = onSettings) { Text("Settings") }
    }
}

@Composable
private fun MacroBar(name: String, logged: Float, target: Int) {
    Text("$name: ${logged.toInt()} / $target g")
    LinearProgressIndicator(
        progress = { (logged / target.coerceAtLeast(1)).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth(),
    )
}