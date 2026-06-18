package com.example.gymbuddy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.gymbuddy.data.AppDatabase
import com.example.gymbuddy.data.ProgressEntry
import com.example.gymbuddy.data.WorkoutPlan
import com.example.gymbuddy.data.repository.GymRepository
import kotlinx.coroutines.launch

class GymViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = GymRepository(AppDatabase.getDatabase(application).gymDao())

    val workoutPlans = repository.getWorkoutPlans().asLiveData()
    val trainingLogs = repository.getTrainingLogs().asLiveData()
    val progressEntries = repository.getProgressEntries().asLiveData()

    init {
        refreshSync()
    }

    fun refreshSync() = viewModelScope.launch {
        repository.refreshSync()
    }

    fun insertWorkoutPlan(plan: WorkoutPlan) = viewModelScope.launch {
        repository.insertWorkoutPlan(plan)
    }

    fun deleteWorkoutPlan(plan: WorkoutPlan) = viewModelScope.launch {
        repository.deleteWorkoutPlan(plan)
    }

    fun insertProgressEntry(entry: ProgressEntry) = viewModelScope.launch {
        repository.insertProgressEntry(entry)
    }

    fun deleteProgressEntry(entry: ProgressEntry) = viewModelScope.launch {
        repository.deleteProgressEntry(entry)
    }

    fun markAttendance(plan: WorkoutPlan, dateOverride: Long? = null) = viewModelScope.launch {
        repository.markAttendance(plan, dateOverride)
    }

    fun clearUserData() = viewModelScope.launch {
        repository.clearUserData()
    }
}
