package com.example.gymbuddy.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "progress_entries")
data class ProgressEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val weightKg: Float,
    val bodyFatPercentage: Float? = null,
    val photoUri: String? = null, // Mantido para compatibilidade, agora será lista separada por vírgulas
    val userId: String = "",
    val notes: String? = null
)
