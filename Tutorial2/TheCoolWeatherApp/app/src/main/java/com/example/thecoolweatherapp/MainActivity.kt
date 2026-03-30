package com.example.thecoolweatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.thecoolweatherapp.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class MainActivity : AppCompatActivity() {

    private val viewModel: WeatherViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            refreshLocation()
        } else {
            setLisbonDefault()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupObservers()
        setupListeners()

        checkLocationPermission()
    }

    private fun setupObservers() {
        viewModel.weatherData.observe(this) { data ->
            data?.let {
                updateWeatherUI(it)
            }
        }

        viewModel.lastUpdateTime.observe(this) { time ->
            binding.timeValue.text = time
        }
        
        viewModel.isDay.observe(this) { 
            viewModel.weatherData.value?.let { updateWeatherUI(it) }
        }
    }

    private fun setupListeners() {
        binding.updateButton.setOnClickListener {
            val latText = binding.latitudeInput.text.toString()
            val lonText = binding.longitudeInput.text.toString()
            
            if (latText.isEmpty() || lonText.isEmpty()) {
                refreshLocation()
            } else {
                val lat = latText.toFloatOrNull()
                val lon = lonText.toFloatOrNull()
                if (lat != null && lon != null) {
                    viewModel.fetchWeather(lat, lon)
                } else {
                    Toast.makeText(this, "Invalid Coordinates", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateWeatherUI(data: WeatherData) {
        val info = viewModel.getWeatherInfo(data.current_weather.weathercode)
        val imageName = info.first
        val isDay = viewModel.isDay.value ?: true
        
        val finalImageName = when (data.current_weather.weathercode) {
            0, 1, 2 -> if (isDay) "${imageName}day" else "${imageName}night"
            else -> imageName
        }

        val resID = resources.getIdentifier(finalImageName, "drawable", packageName)
        if (resID != 0) {
            binding.weatherImage.setImageResource(resID)
        }

        binding.pressureValue.text = "${data.hourly.pressure_msl[0]} hPa"
        binding.temperatureValue.text = "${data.current_weather.temperature} °C"
        binding.windSpeedValue.text = "${data.current_weather.windspeed} km/h"
        binding.windDirectionValue.text = "${data.current_weather.winddirection}"
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            refreshLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    private fun refreshLocation() {
        try {
            // Priority.PRIORITY_HIGH_ACCURACY forces the emulator to look at the new location
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        binding.latitudeInput.setText(String.format("%.4f", location.latitude))
                        binding.longitudeInput.setText(String.format("%.4f", location.longitude))
                        viewModel.fetchWeather(location.latitude.toFloat(), location.longitude.toFloat())
                    } else {
                        setLisbonDefault()
                    }
                }
        } catch (e: SecurityException) {
            setLisbonDefault()
        }
    }

    private fun setLisbonDefault() {
        binding.latitudeInput.setText("38.76")
        binding.longitudeInput.setText("-9.12")
        viewModel.fetchWeather(38.76f, -9.12f)
    }
}
