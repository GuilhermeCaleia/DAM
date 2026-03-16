package com.example.wheatherapp.data.api

import com.example.wheatherapp.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("weather")
    suspend fun getWeather(
        @Query("lat") lat: Float,
        @Query("lon") lon: Float,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse

    companion object {
        const val BASE_URL = "https://api.openweathermap.org/data/2.5/"
    }
}
