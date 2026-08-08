package com.youngjcu.pclab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youngjcu.pclab.data.repository.HardwareRepository
import com.youngjcu.pclab.data.repository.LearningRepository
import com.youngjcu.pclab.data.repository.LearningStatistics
import com.youngjcu.pclab.data.repository.SettingsRepository
import com.youngjcu.pclab.data.repository.ThemePreference
import com.youngjcu.pclab.data.repository.UserSettings
import com.youngjcu.pclab.domain.model.BuildDraft
import com.youngjcu.pclab.domain.model.Evaluation
import com.youngjcu.pclab.domain.model.HardwareCatalogue
import com.youngjcu.pclab.domain.model.HardwarePart
import com.youngjcu.pclab.domain.model.Mission
import com.youngjcu.pclab.domain.rules.BuildEvaluator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUiState(
    val catalogue: HardwareCatalogue? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedMission: Mission? = null,
    val draft: BuildDraft = BuildDraft(),
    val evaluation: Evaluation? = null,
    val statistics: LearningStatistics = LearningStatistics(),
    val settings: UserSettings = UserSettings()
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val hardwareRepository: HardwareRepository,
    private val learningRepository: LearningRepository,
    private val settingsRepository: SettingsRepository,
    private val evaluator: BuildEvaluator
) : ViewModel() {
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        loadCatalogue()
        viewModelScope.launch {
            learningRepository.observeStatistics().collect { statistics ->
                _state.update { it.copy(statistics = statistics) }
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
    }

    fun loadCatalogue() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        hardwareRepository.fetchCatalogue()
            .onSuccess { catalogue -> _state.update { it.copy(catalogue = catalogue, isLoading = false) } }
            .onFailure { error ->
                _state.update {
                    it.copy(isLoading = false, errorMessage = "We could not load the learning catalogue. Check your connection and try again.")
                }
            }
    }

    fun startMission(missionId: Int) {
        val mission = _state.value.catalogue?.missions?.firstOrNull { it.id == missionId } ?: return
        _state.update { it.copy(selectedMission = mission, draft = BuildDraft(), evaluation = null) }
    }

    fun selectPart(part: HardwarePart) {
        _state.update { it.copy(draft = it.draft.withPart(part), evaluation = null) }
    }

    fun submitBuild() {
        val current = _state.value
        val mission = current.selectedMission ?: return
        val evaluation = evaluator.evaluate(current.draft, mission)
        _state.update { it.copy(evaluation = evaluation) }
        viewModelScope.launch { learningRepository.saveResult(mission, current.draft, evaluation) }
    }

    fun saveFavourite() = viewModelScope.launch {
        val mission = _state.value.selectedMission ?: return@launch
        learningRepository.saveFavourite("${mission.title} build", _state.value.draft)
    }

    fun updateTheme(theme: ThemePreference) = viewModelScope.launch { settingsRepository.updateTheme(theme) }
    fun updateFontScale(scale: Float) = viewModelScope.launch { settingsRepository.updateFontScale(scale) }
    fun updateColourBlindMode(enabled: Boolean) = viewModelScope.launch { settingsRepository.updateColourBlindMode(enabled) }
    fun resetProgress() = viewModelScope.launch { learningRepository.resetLearningData() }
}
