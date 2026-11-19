package com.fatflow.app.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fatflow.app.data.mondayOf
import com.fatflow.app.di.AppContainer
import com.fatflow.app.domain.GaugeStatus
import com.fatflow.app.domain.MetabolicEngine
import com.fatflow.app.domain.SessionKind
import com.fatflow.app.ui.labels.dayTypeLabel
import com.fatflow.app.ui.labels.gaugeCaption
import com.fatflow.app.ui.labels.preWorkoutLabel
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val date: LocalDate = LocalDate.now(),
    val dayTypeLabel: String = "",
    val deficitKcal: Int = 0,
    val maxFatReleaseKcal: Int = 0,
    val gaugeStatus: GaugeStatus = GaugeStatus.NO_INTAKE,
    val gaugeCaption: String = "",
    val gaugeFraction: Float = 0f,
    val proteinTarget: Int = 0,
    val proteinLogged: Float = 0f,
    val carbTarget: Int = 0,
    val carbLogged: Float = 0f,
    val fatTarget: Int = 0,
    val fatLogged: Float = 0f,
    val preWorkoutCarbGrams: Int = 0,
    val preWorkoutLabel: String = "",
    val isDietBreak: Boolean = false,
    val needsCheckIn: Boolean = false,
    val showEmptyTarget: Boolean = false,
)

class TodayViewModel(private val container: AppContainer) : ViewModel() {
    private val today = LocalDate.now()

    val state: StateFlow<TodayUiState> =
        combine(
            container.targets.observe(today),
            container.foodLogs.observeDay(today),
            container.weekPlan.observeWeek(mondayOf(today)),
            container.bodyMetrics.observeLatest(),
        ) { target, logs, plans, metric ->
            if (target == null) {
                return@combine TodayUiState(showEmptyTarget = true)
            }
            val intake = logs.sumOf { it.calories }
            val deficit = target.estimatedTdee - intake
            val status =
                MetabolicEngine.gaugeStatus(target.isDietBreak, intake, deficit, target.maxFatReleaseKcal)
            val fraction =
                when {
                    status == GaugeStatus.DIET_BREAK || status == GaugeStatus.NO_INTAKE ||
                        target.maxFatReleaseKcal == 0 -> 0f
                    deficit <= 0 -> 0f
                    else -> (deficit.toFloat() / target.maxFatReleaseKcal).coerceIn(0f, 1f)
                }
            val plan = plans.firstOrNull { it.date == today }
            val session = plan?.sessionKind ?: SessionKind.REST
            val lbm = metric?.leanMassLbs?.toDouble() ?: 140.0
            val label = dayTypeLabel(target.dayType, session)
            val pre =
                MetabolicEngine.preWorkoutCarbGrams(
                    lbm,
                    session,
                    target.carbGrams,
                    logs.sumOf { it.carbGrams.toDouble() }.toInt(),
                )
            val needsCheckIn = metric == null || metric.date != today
            TodayUiState(
                date = today,
                dayTypeLabel = label,
                deficitKcal = deficit,
                maxFatReleaseKcal = target.maxFatReleaseKcal,
                gaugeStatus = status,
                gaugeCaption = gaugeCaption(status),
                gaugeFraction = fraction,
                proteinTarget = target.proteinGrams,
                proteinLogged = logs.sumOf { it.proteinGrams.toDouble() }.toFloat(),
                carbTarget = target.carbGrams,
                carbLogged = logs.sumOf { it.carbGrams.toDouble() }.toFloat(),
                fatTarget = target.fatGrams,
                fatLogged = logs.sumOf { it.fatGrams.toDouble() }.toFloat(),
                preWorkoutCarbGrams = pre,
                preWorkoutLabel = preWorkoutLabel(session, pre, plan?.startTime),
                isDietBreak = target.isDietBreak,
                needsCheckIn = needsCheckIn,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun overrideDayType() {
        viewModelScope.launch { container.targets.overrideDayType(today) }
    }

    fun repeatPreWorkout(onEmpty: () -> Unit) {
        viewModelScope.launch {
            val n = container.foodLogs.repeatYesterdayPreWorkout(today)
            if (n == 0) onEmpty()
        }
    }

    fun recalculate() {
        viewModelScope.launch { container.recomputer.recompute(today) }
    }
}

class TodayViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = TodayViewModel(container) as T
}