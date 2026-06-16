package com.example.pantrypal.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrypal.data.settings.SettingsRepository
import com.example.pantrypal.domain.model.StorageLocationFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _uiState.update { it.copy(username = settings.username.takeIf(String::isNotBlank)) }
            }
        }
    }

    fun onEvent(event: HomeEvent) {
        viewModelScope.launch {
            when (event) {
                is HomeEvent.OnExpiringFoodClick -> _effects.send(
                    HomeEffect.NavigateToPantry(StorageLocationFilter.ALL, event.categoryId)
                )
                is HomeEvent.OnStorageStatClick -> _effects.send(HomeEffect.NavigateToPantry(event.filter))
                is HomeEvent.OnRecipeClick -> _effects.send(HomeEffect.NavigateToRecipeDetail(event.recipeId))
                HomeEvent.OnFabClick -> _effects.send(HomeEffect.OpenAddChoiceSheet)
            }
        }
    }
}
