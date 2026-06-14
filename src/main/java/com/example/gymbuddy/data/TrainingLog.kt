package com.example.gymbuddy.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "training_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlan::class,
            parentColumns = ["id"],
            childColumns = ["workoutPlanId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TrainingLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long, // Timestamp
    val attended: Boolean,
    val workoutPlanId: Long,
    val streakCount: Int,
    val isAdditional: Boolean = false,
    val userId: String = ""
)
