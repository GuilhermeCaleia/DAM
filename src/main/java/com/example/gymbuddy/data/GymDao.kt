package com.example.gymbuddy.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    // Workout Plans
    @Query("SELECT * FROM workout_plans WHERE userId = :userId")
    fun getAllWorkoutPlans(userId: String): Flow<List<WorkoutPlan>>

    @Query("SELECT * FROM workout_plans WHERE userId = :userId")
    suspend fun getAllWorkoutPlansList(userId: String): List<WorkoutPlan>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutPlan(plan: WorkoutPlan)

    @Delete
    suspend fun deleteWorkoutPlan(plan: WorkoutPlan)

    @Delete
    suspend fun deleteTrainingLog(log: TrainingLog)

    @Delete
    suspend fun deleteProgressEntry(entry: ProgressEntry)

    @Query("DELETE FROM workout_plans WHERE isAdditional = 1 AND createdAt < :timestamp AND userId = :userId")
    suspend fun deleteOldAdditionalPlans(userId: String, timestamp: Long)

    // Training Logs
    @Query("SELECT * FROM training_logs WHERE userId = :userId ORDER BY date DESC")
    fun getAllTrainingLogs(userId: String): Flow<List<TrainingLog>>

    @Query("SELECT * FROM training_logs WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllTrainingLogsList(userId: String): List<TrainingLog>

    @Query("SELECT * FROM progress_entries WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllProgressEntriesList(userId: String): List<ProgressEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrainingLog(log: TrainingLog)

    // Progress Entries
    @Query("SELECT * FROM progress_entries WHERE userId = :userId ORDER BY date DESC")
    fun getAllProgressEntries(userId: String): Flow<List<ProgressEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressEntry(entry: ProgressEntry)

    // Clear Data
    @Query("DELETE FROM workout_plans WHERE userId = :userId")
    suspend fun clearAllWorkoutPlans(userId: String)

    @Query("DELETE FROM training_logs WHERE userId = :userId")
    suspend fun clearAllTrainingLogs(userId: String)

    @Query("DELETE FROM progress_entries WHERE userId = :userId")
    suspend fun clearAllProgressEntries(userId: String)
}
