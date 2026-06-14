package com.example.gymbuddy.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_plans",
    indices = [Index(value = ["name", "userId"], unique = true)]
)
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val days: String,
    val muscleGroups: String,
    val hour: String,
    val isAdditional: Boolean = false,
    val userId: String = "",
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
