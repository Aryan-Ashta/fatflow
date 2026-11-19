package com.fatflow.app.ui.week

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import com.fatflow.app.domain.SessionKind
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekScreen(vm: WeekViewModel) {
    val state by vm.state.collectAsState()
    var timeEditDate by remember { mutableStateOf<LocalDate?>(null) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { vm.previousWeek() }) { Text("←") }
            Text("Week of ${state.weekStart}")
            TextButton(onClick = { vm.nextWeek() }) { Text("→") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SessionKind.entries.forEach { kind ->
                FilterChip(
                    selected = state.selectedSession == kind,
                    onClick = { vm.selectSession(kind) },
                    label = {
                        Text(
                            when (kind) {
                                SessionKind.TENNIS -> "Tennis"
                                SessionKind.LIFTING -> "Lifting"
                                SessionKind.DOUBLE -> "Double"
                                SessionKind.REST -> "Rest"
                            },
                        )
                    },
                )
            }
        }
        state.dietBreakBanner?.let { Text(it) }
        state.rows.forEach { row ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (state.selectedSession != null) vm.applySessionToDate(row.date)
                    }
                    .padding(vertical = 4.dp),
            ) {
                Text(
                    "${row.weekday}: ${sessionLabel(row.sessionKind)} · ${row.dayTypeLabel} · ${row.kcal} kcal",
                )
                TextButton(onClick = { timeEditDate = row.date }) {
                    Text(row.startTime?.toString() ?: "Set time")
                }
            }
        }
        Text(state.footer)
    }
    timeEditDate?.let { date ->
        val pickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { timeEditDate = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.setStartTime(
                            date,
                            LocalTime.of(pickerState.hour, pickerState.minute),
                        )
                        timeEditDate = null
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { timeEditDate = null }) { Text("Cancel") }
            },
            text = { TimePicker(pickerState) },
        )
    }
}

private fun sessionLabel(kind: SessionKind): String =
    when (kind) {
        SessionKind.TENNIS -> "Tennis"
        SessionKind.LIFTING -> "Lifting"
        SessionKind.DOUBLE -> "Double Session"
        SessionKind.REST -> "Rest"
    }