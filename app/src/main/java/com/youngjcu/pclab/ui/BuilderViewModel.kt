package com.youngjcu.pclab.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youngjcu.pclab.data.repository.HardwareRepository
import com.youngjcu.pclab.domain.model.HardwareCatalogue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BuilderUiState(
    val catalogue: HardwareCatalogue? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/** Owns the Builder screen's catalogue state and receives it through HardwareRepository. */
@HiltViewModel
class BuilderViewModel @Inject constructor(
    private val hardwareRepository: HardwareRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BuilderUiState())
    val state: StateFlow<BuilderUiState> = _state.asStateFlow()

    init {
        refreshCatalogue()
    }

    fun refreshCatalogue() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        hardwareRepository.fetchCatalogue()
            .onSuccess { catalogue -> _state.update { it.copy(catalogue = catalogue, isLoading = false) } }
            .onFailure {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to load hardware data from GitHub Raw. Try again when online."
                    )
                }
            }
    }
}
