package com.fatflow.app.ui.trajectory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fatflow.app.data.computeAdherence
import com.fatflow.app.di.AppContainer
import com.fatflow.app.domain.MetabolicEngine
import com.fatflow.app.domain.TrajectoryPoint
import com.fatflow.app.domain.TrajectoryProjector
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class TrajectoryUiState(
    val points: List<TrajectoryPoint> = emptyList(),
    val weeksRemaining: Int = 0,
    val energyLabel: String? = null,
)

class TrajectoryViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(TrajectoryUiState())
    val state: StateFlow<TrajectoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                container.bodyMetrics.observeLatest(),
                container.profileStore.profile,
                container.checkIns.observeLatest(),
            ) { metric, profile, checkIn ->
                Triple(metric, profile, checkIn)
            }.collect { (metric, profile, checkIn) ->
                val energy = checkIn?.let { "Energy ${it.energyScore}/5" }
                if (metric == null || !profile.onboardingComplete) {
                    _state.value = TrajectoryUiState(energyLabel = energy)
                    return@collect
                }
                val adherence =
                    computeAdherence(
                        LocalDate.now(),
                        container.database.foodLogDao(),
                        container.database.dailyTargetDao(),
                    )
                val todayTarget = container.database.dailyTargetDao().get(LocalDate.now())
                val tdee =
                    todayTarget?.estimatedTdee
                        ?: MetabolicEngine.seedTdee(metric.leanMassLbs.toDouble())
                val points =
                    TrajectoryProjector.project(
                        leanMassLbs = metric.leanMassLbs.toDouble(),
                        fatMassLbs = metric.fatMassLbs.toDouble(),
                        tdee = tdee,
                        releaseRate = profile.releaseRate,
                        adherence = adherence,
                        goalWeightLbs = profile.goalWeightLbs.toDouble(),
                    )
                _state.value =
                    TrajectoryUiState(
                        points = points,
                        weeksRemaining = points.lastOrNull()?.week ?: 0,
                        energyLabel = energy,
                    )
            }
        }
    }
}

class TrajectoryViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = TrajectoryViewModel(container) as T
}