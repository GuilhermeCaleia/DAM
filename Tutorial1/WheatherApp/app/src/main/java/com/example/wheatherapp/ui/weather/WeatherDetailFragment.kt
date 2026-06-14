package com.example.wheatherapp.ui.weather

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import coil.load
import com.example.wheatherapp.R
import com.example.wheatherapp.databinding.FragmentWeatherDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WeatherDetailFragment : Fragment() {

    private var _binding: FragmentWeatherDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WeatherDetailViewModel by viewModels()
    private val args: WeatherDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWeatherDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.fetchWeather(args.lat, args.lon)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }

        binding.retryButton.setOnClickListener {
            viewModel.fetchWeather(args.lat, args.lon)
        }
    }

    private fun updateUi(state: WeatherDetailViewModel.WeatherUiState) {
        when (state) {
            is WeatherDetailViewModel.WeatherUiState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.weatherCard.visibility = View.GONE
                binding.retryButton.visibility = View.GONE
            }
            is WeatherDetailViewModel.WeatherUiState.Success -> {
                binding.progressBar.visibility = View.GONE
                binding.weatherCard.visibility = View.VISIBLE
                binding.retryButton.visibility = View.GONE

                val weather = state.weather
                binding.locationName.text = weather.name
                binding.temperature.text = getString(R.string.temp_unit).let { "${weather.main.temp.toInt()}$it" }
                binding.description.text = weather.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() }
                binding.humidity.text = getString(R.string.humidity, weather.main.humidity)
                binding.windSpeed.text = getString(R.string.wind_speed, weather.wind.speed)

                val iconCode = weather.weather.firstOrNull()?.icon
                if (iconCode != null) {
                    val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
                    binding.weatherIcon.load(iconUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_launcher_foreground) // Placeholder
                    }
                }
            }
            is WeatherDetailViewModel.WeatherUiState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.weatherCard.visibility = View.GONE
                binding.retryButton.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
