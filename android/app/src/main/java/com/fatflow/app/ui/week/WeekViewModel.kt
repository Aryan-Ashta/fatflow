package com.fatflow.app.ui.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fatflow.app.data.mondayOf
import com.fatflow.app.di.AppContainer
import com.fatflow.app.domain.MetabolicEngine
import com.fatflow.app.domain.SessionKind
import com.fatflow.app.ui.labels.dayTypeName
import com.fatflow.app.ui.labels.sessionDisplay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeekRow(
    val date: LocalDate,
    val weekday: String,
    val sessionKind: SessionKind,
    val dayTypeLabel: String,
    val kcal: Int,
    val startTime: LocalTime?,
    val isDietBreak: Boolean,
)

data class WeekUiState(
    val weekStart: LocalDate = mondayOf(LocalDate.now()),
    val rows: List<WeekRow> = emptyList(),
    val selectedSession: SessionKind? = null,
    val dietBreakBanner: String? = null,
    val footer: String = "",
)

class WeekViewModel(private val container: AppContainer) : ViewModel() {
    private val weekStart = MutableStateFlow(mondayOf(LocalDate.now()))
    val selectedSession = MutableStateFlow<SessionKind?>(null)

    val state: StateFlow<WeekUiState> =
        weekStart
            .flatMapLatest { start ->
                val end = start.plusDays(6)
                combine(
                    container.weekPlan.observeWeek(start),
                    container.targets.observeWeek(start),
                    container.profileStore.profile,
                    container.bodyMetrics.observeLatest(),
                    selectedSession,
                ) { plans, targets, profile, metric, picked ->
                    val rows =
                        (0..6).map { i ->
                            val date = start.plusDays(i.toLong())
                            val plan = plans.firstOrNull { it.date == date }
                            val target = targets.firstOrNull { it.date == date }
                            val kind = plan?.sessionKind ?: SessionKind.REST
                            WeekRow(
                                date = date,
                                weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US),
                                sessionKind = kind,
                                dayTypeLabel = target?.let { dayTypeName(it.dayType) } ?: "—",
                                kcal = target?.targetCalories ?: 0,
                                startTime = plan?.startTime,
                                isDietBreak = target?.isDietBreak == true,
                            )
                        }
                    val breakRow = rows.firstOrNull { it.isDietBreak }
                    val banner =
                        if (breakRow != null) {
                            val startBreak = rows.first { it.isDietBreak }.date
                            val endBreak = rows.last { it.isDietBreak }.date
                            "Maintenance refeed $startBreak – $endBreak"
                        } else {
                            null
                        }
                    val sum = rows.sumOf { it.kcal }
                    val todayTarget = targets.firstOrNull { it.date == LocalDate.now() }
                    val lbm = metric?.leanMassLbs?.toDouble() ?: profile.startWeightLbs.toDouble()
                    val fm = metric?.fatMassLbs?.toDouble() ?: profile.startFatMassLbs.toDouble()
                    val tdee = todayTarget?.estimatedTdee ?: MetabolicEngine.seedTdee(lbm)
                    val baseline =
                        MetabolicEngine.calorieTarget(
                            tdee,
                            MetabolicEngine.maxFatReleaseKcal(fm, profile.releaseRate),
                            MetabolicEngine.safeFloorKcal(lbm),
                        )
                    WeekUiState(
                        weekStart = start,
                        rows = rows,
                        selectedSession = picked,
                        dietBreakBanner = banner,
                        footer = "Week locked to $sum kcal (target ${baseline * 7})",
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeekUiState())

    fun previousWeek() {
        weekStart.value = weekStart.value.minusDays(7)
    }

    fun nextWeek() {
        weekStart.value = weekStart.value.plusDays(7)
    }

    fun selectSession(kind: SessionKind) {
        selectedSession.value = kind
    }

    fun applySessionToDate(date: LocalDate) {
        val kind = selectedSession.value ?: return
        viewModelScope.launch {
            container.weekPlan.setSession(date, kind)
            selectedSession.value = null
        }
    }

    fun setStartTime(date: LocalDate, time: LocalTime) {
        viewModelScope.launch { container.weekPlan.setStartTime(date, time) }
    }
}

class WeekViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = WeekViewModel(container) as T
}