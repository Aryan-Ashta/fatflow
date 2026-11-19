package com.fatflow.app.ui.checkin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fatflow.app.di.AppContainer
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    container: AppContainer,
    onDone: () -> Unit,
) {
    var weight by remember { mutableStateOf("") }
    var bf by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var energy by remember { mutableIntStateOf(3) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.padding(16.dp)) {
        Text("Weekly check-in")
        OutlinedTextField(weight, { weight = it }, label = { Text("Weight (lb)") })
        OutlinedTextField(bf, { bf = it }, label = { Text("Body fat % (optional)") })
        OutlinedTextField(waist, { waist = it }, label = { Text("Waist inches (optional)") })
        Text("Energy")
        SingleChoiceSegmentedButtonRow {
            (1..5).forEach { score ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = score - 1, count = 5),
                    onClick = { energy = score },
                    selected = energy == score,
                ) { Text(score.toString()) }
            }
        }
        error?.let { Text(it) }
        Button(
            onClick = {
                val w = weight.toDoubleOrNull()
                if (w == null || w !in 80.0..500.0) {
                    error = "Enter a weight between 80 and 500 lb."
                    return@Button
                }
                val bfVal = bf.toDoubleOrNull()
                if (bf != "" && (bfVal == null || bfVal !in 3.0..60.0)) {
                    error = "Enter body fat between 3 and 60%."
                    return@Button
                }
                val waistVal = waist.toDoubleOrNull()
                if (waist != "" && (waistVal == null || waistVal !in 20.0..80.0)) {
                    error = "Enter a waist between 20 and 80 in."
                    return@Button
                }
                scope.launch {
                    container.checkIns.save(
                        LocalDate.now(),
                        w,
                        bfVal,
                        waistVal,
                        energy,
                    )
                    onDone()
                }
            },
        ) { Text("Save") }
    }
}