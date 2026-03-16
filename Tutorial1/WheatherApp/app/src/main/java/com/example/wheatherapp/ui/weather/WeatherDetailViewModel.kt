package com.example.wheatherapp.ui.weather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wheatherapp.data.model.WeatherResponse
import com.example.wheatherapp.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherDetailViewModel @Inject constructor(
    private val repository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState

    fun fetchWeather(lat: Float, lon: Float) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val response = repository.getWeather(lat, lon)
                _uiState.value = WeatherUiState.Success(response)
            } catch (e: Exception) {
                _uiState.value = WeatherUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    sealed class WeatherUiState {
        object Loading : WeatherUiState()
        data class Success(val weather: WeatherResponse) : WeatherUiState()
        data class Error(val message: String) : WeatherUiState()
    }
}
