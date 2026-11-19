package com.fatflow.app.ui.trajectory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun TrajectoryScreen(
    vm: TrajectoryViewModel,
    onCheckIn: () -> Unit,
) {
    val state by vm.state.collectAsState()
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Column(Modifier.padding(16.dp)) {
        Text("About ${state.weeksRemaining} weeks remaining")
        state.energyLabel?.let { Text(it) }
        val pts = state.points
        if (pts.size >= 2) {
            Canvas(Modifier.fillMaxWidth().height(200.dp)) {
                val minW = pts.minOf { it.weightLbs }
                val maxW = pts.maxOf { it.weightLbs }
                val span = (maxW - minW).coerceAtLeast(1.0)
                val weightPath = Path()
                val calPath = Path()
                pts.forEachIndexed { i, p ->
                    val x = size.width * i / (pts.size - 1).coerceAtLeast(1)
                    val yW = size.height * (1f - ((p.weightLbs - minW) / span).toFloat())
                    val maxCal = pts.maxOf { it.calorieTarget }.coerceAtLeast(1)
                    val yC = size.height * (1f - p.calorieTarget.toFloat() / maxCal)
                    val ptW = Offset(x, yW)
                    val ptC = Offset(x, yC)
                    if (i == 0) {
                        weightPath.moveTo(ptW.x, ptW.y)
                        calPath.moveTo(ptC.x, ptC.y)
                    } else {
                        weightPath.lineTo(ptW.x, ptW.y)
                        calPath.lineTo(ptC.x, ptC.y)
                    }
                }
                drawPath(weightPath, primary)
                drawPath(calPath, tertiary)
            }
        }
        Button(onClick = onCheckIn) { Text("Check in") }
    }
}