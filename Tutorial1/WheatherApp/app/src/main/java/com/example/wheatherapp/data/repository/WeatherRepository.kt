package com.example.wheatherapp.data.repository

import com.example.wheatherapp.data.api.WeatherApi
import com.example.wheatherapp.data.model.WeatherResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApi
) {
    suspend fun getWeather(lat: Float, lon: Float): WeatherResponse {
        // Replace with your actual API key or manage it via BuildConfig/Secrets
        val apiKey = "d93eb5b109817a45000bb6cf98cd92f4"
        return weatherApi.getWeather(lat, lon, apiKey)
    }
}
