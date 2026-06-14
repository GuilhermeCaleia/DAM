package com.example.thecoolweatherapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WeatherRepository()

    private val _weatherData = MutableLiveData<WeatherData?>()
    val weatherData: LiveData<WeatherData?> = _weatherData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    val latitude = MutableLiveData<String>()
    val longitude = MutableLiveData<String>()

    private val _lastUpdateTime = MutableLiveData<String>()
    val lastUpdateTime: LiveData<String> = _lastUpdateTime

    private val _isDay = MutableLiveData<Boolean>(true)
    val isDay: LiveData<Boolean> = _isDay

    fun fetchWeather(lat: Float, lon: Float) {
        _isLoading.value = true
        
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        _lastUpdateTime.postValue(sdf.format(Date()))

        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.fetchWeather(lat, lon)
            
            result?.let { data ->
                val currentT = Date().time
                val sdfFull = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
                
                try {
                    val sunriseStr = data.daily?.sunrise?.get(0)
                    val sunsetStr = data.daily?.sunset?.get(0)
                    
                    if (sunriseStr != null && sunsetStr != null) {
                        val sunrise = sdfFull.parse(sunriseStr)?.time ?: 0L
                        val sunset = sdfFull.parse(sunsetStr)?.time ?: 0L
                        _isDay.postValue(currentT in sunrise..sunset)
                    } else {
                        throw Exception("Sunrise/Sunset null")
                    }
                } catch (e: Exception) {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    _isDay.postValue(hour in 7..19)
                }
            }

            _weatherData.postValue(result)
            _isLoading.postValue(false)
        }
    }

    fun getWeatherInfo(code: Int): Triple<String, String, String> {
        val resources = getApplication<Application>().resources
        val codes = resources.getIntArray(R.array.weather_codes)
        val images = resources.getStringArray(R.array.weather_images)
        val descriptions = resources.getStringArray(R.array.weather_descriptions)

        val index = codes.indexOf(code)
        return if (index != -1) {
            Triple(images[index], descriptions[index], "")
        } else {
            Triple("fog", "Unknown", "")
        }
    }
}
