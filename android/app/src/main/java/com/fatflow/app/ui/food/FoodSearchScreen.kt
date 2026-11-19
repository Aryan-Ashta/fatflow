package com.fatflow.app.ui.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fatflow.app.data.local.entity.FoodLogEntity
import com.fatflow.app.data.repo.CachedFood
import com.fatflow.app.di.AppContainer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun FoodSearchScreen(container: AppContainer, onDone: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CachedFood>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var manualName by remember { mutableStateOf("") }
    var manualGrams by remember { mutableStateOf("100") }
    var manualKcal by remember { mutableStateOf("") }
    var manualP by remember { mutableStateOf("") }
    var manualC by remember { mutableStateOf("") }
    var manualF by remember { mutableStateOf("") }
    var savePreset by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(query, { query = it }, label = { Text("Search or barcode") })
        Button(
            onClick = {
                scope.launch {
                    val r = container.foodSearch.search(query)
                    results = r.foods
                    error = r.error
                }
            },
        ) { Text("Search") }
        error?.let { Text(it) }
        LazyColumn {
            items(results) { food ->
                Text(
                    "${food.name} (${food.kcalPer100g.toInt()} kcal/100g)",
                    Modifier.clickable {
                        scope.launch {
                            logFood(container, food, 100.0, today, onDone)
                        }
                    },
                )
            }
        }
        Text("Manual entry")
        OutlinedTextField(manualName, { manualName = it }, label = { Text("Name") })
        OutlinedTextField(manualGrams, { manualGrams = it }, label = { Text("Grams") })
        OutlinedTextField(manualKcal, { manualKcal = it }, label = { Text("Calories") })
        OutlinedTextField(manualP, { manualP = it }, label = { Text("Protein g") })
        OutlinedTextField(manualC, { manualC = it }, label = { Text("Carbs g") })
        OutlinedTextField(manualF, { manualF = it }, label = { Text("Fat g") })
        val gramsVal = manualGrams.toDoubleOrNull() ?: 0.0
        if (gramsVal > 0) {
            RowCheckbox(savePreset, { savePreset = it }, "Save as preset")
        }
        Button(
            onClick = {
                scope.launch {
                    val kcal = manualKcal.toIntOrNull() ?: 0
                    val p = manualP.toFloatOrNull() ?: 0f
                    val c = manualC.toFloatOrNull() ?: 0f
                    val f = manualF.toFloatOrNull() ?: 0f
                    if (savePreset && gramsVal > 0) {
                        container.foodSearch.savePreset(manualName, gramsVal, kcal, p, c, f)
                    }
                    container.foodLogs.log(
                        FoodLogEntity(
                            date = today,
                            mealName = manualName,
                            calories = kcal,
                            proteinGrams = p,
                            carbGrams = c,
                            fatGrams = f,
                            source = "MANUAL",
                            grams = gramsVal.toFloat(),
                            isPreWorkout = defaultIsPreWorkout(container),
                        ),
                    )
                    onDone()
                }
            },
        ) { Text("Save log") }
    }
}

@Composable
private fun RowCheckbox(checked: Boolean, onChange: (Boolean) -> Unit, label: String) {
    androidx.compose.foundation.layout.Row {
        Checkbox(checked, onChange)
        Text(label)
    }
}

private suspend fun logFood(
    container: AppContainer,
    food: CachedFood,
    grams: Double,
    today: LocalDate,
    onDone: () -> Unit,
) {
    container.foodLogs.log(
        FoodLogEntity(
            date = today,
            mealName = food.name,
            calories = (food.kcalPer100g * grams / 100.0).roundToInt(),
            proteinGrams = food.proteinPer100g * grams.toFloat() / 100f,
            carbGrams = food.carbPer100g * grams.toFloat() / 100f,
            fatGrams = food.fatPer100g * grams.toFloat() / 100f,
            source = "SEARCH",
            barcode = food.barcode,
            grams = grams.toFloat(),
            isPreWorkout = defaultIsPreWorkout(container),
        ),
    )
    onDone()
}
