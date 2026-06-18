package com.example.gymbuddy.data.repository

import android.net.Uri
import com.example.gymbuddy.data.GymDao
import com.example.gymbuddy.data.ProgressEntry
import com.example.gymbuddy.data.TrainingLog
import com.example.gymbuddy.data.WorkoutPlan
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class GymRepository(
    private val dao: GymDao,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    private val auth = Firebase.auth
    private var isSyncing = false

    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    fun getWorkoutPlans(): Flow<List<WorkoutPlan>> = dao.getAllWorkoutPlans(currentUserId)

    fun getTrainingLogs(): Flow<List<TrainingLog>> = dao.getAllTrainingLogs(currentUserId)

    fun getProgressEntries(): Flow<List<ProgressEntry>> = dao.getAllProgressEntries(currentUserId)

    suspend fun refreshSync() {
        val userId = currentUserId
        if (userId.isEmpty() || isSyncing) return

        isSyncing = true
        try {
            cleanupOldAdditionalPlans(userId)
            syncWorkoutPlans(userId)
            syncTrainingLogs(userId)
            syncProgressEntries(userId)
        } finally {
            isSyncing = false
        }
    }

    suspend fun insertWorkoutPlan(plan: WorkoutPlan) {
        val userId = currentUserId
        if (userId.isEmpty()) return

        dao.insertWorkoutPlan(plan.copy(userId = userId))
        db.collection("users").document(userId).collection("workout_plans").document(plan.name)
            .set(
                hashMapOf(
                    "name" to plan.name,
                    "days" to plan.days,
                    "muscleGroups" to plan.muscleGroups,
                    "hour" to plan.hour,
                    "isAdditional" to plan.isAdditional
                )
            )
    }

    suspend fun deleteWorkoutPlan(plan: WorkoutPlan) {
        val userId = currentUserId
        if (userId.isEmpty()) return

        dao.deleteWorkoutPlan(plan)
        runCatching {
            db.collection("users").document(userId)
                .collection("workout_plans").document(plan.name)
                .delete().await()
        }
    }

    suspend fun insertProgressEntry(entry: ProgressEntry) {
        val userId = currentUserId
        if (userId.isEmpty()) return

        val cloudUrls = mutableListOf<String>()
        val localUris = entry.photoUri?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

        for (localUri in localUris) {
            if (localUri.startsWith("content://")) {
                runCatching {
                    val timestamp = System.currentTimeMillis()
                    val ref = storage.reference.child("progress_images/$userId/${entry.date}_$timestamp.jpg")
                    ref.putFile(Uri.parse(localUri)).await()
                    cloudUrls.add(ref.downloadUrl.await().toString())
                }
            } else {
                cloudUrls.add(localUri)
            }
        }

        val cloudUrlsString = cloudUrls.takeIf { it.isNotEmpty() }?.joinToString(",")
        val finalEntry = entry.copy(userId = userId, photoUri = cloudUrlsString)
        dao.insertProgressEntry(finalEntry)

        db.collection("users").document(userId).collection("progress_entries").document(entry.date.toString())
            .set(
                hashMapOf(
                    "date" to entry.date,
                    "weightKg" to entry.weightKg,
                    "bodyFatPercentage" to entry.bodyFatPercentage,
                    "photoUri" to cloudUrlsString,
                    "notes" to entry.notes
                )
            )
    }

    suspend fun deleteProgressEntry(entry: ProgressEntry) {
        val userId = currentUserId
        if (userId.isEmpty()) return

        dao.deleteProgressEntry(entry)
        runCatching {
            db.collection("users").document(userId)
                .collection("progress_entries").document(entry.date.toString())
                .delete().await()

            entry.photoUri?.split(",")
                ?.filter { it.contains("firebaseapp.com") || it.contains("googleapis.com") }
                ?.forEach { storage.getReferenceFromUrl(it).delete().await() }
        }
    }

    suspend fun markAttendance(plan: WorkoutPlan, dateOverride: Long? = null) {
        val userId = currentUserId
        if (userId.isEmpty()) return

        val targetDate = dateOverride ?: System.currentTimeMillis()
        val allLogs = dao.getAllTrainingLogsList(userId)
        if (allLogs.any { it.workoutPlanId == plan.id && isSameDay(it.date, targetDate) }) return

        val lastLog = allLogs.firstOrNull()
        var newStreak = 1
        if (lastLog != null) {
            val missed = checkMissedConstantWorkouts(
                start = Calendar.getInstance().apply { timeInMillis = lastLog.date },
                end = Calendar.getInstance().apply { timeInMillis = targetDate },
                plans = dao.getAllWorkoutPlansList(userId),
                logs = allLogs
            )
            newStreak = if (missed) 1 else lastLog.streakCount + 1
        }

        dao.insertTrainingLog(
            TrainingLog(
                date = targetDate,
                attended = true,
                workoutPlanId = plan.id,
                streakCount = newStreak,
                isAdditional = plan.isAdditional,
                userId = userId
            )
        )
        db.collection("users").document(userId).collection("training_logs").document(targetDate.toString())
            .set(
                hashMapOf(
                    "date" to targetDate,
                    "planName" to plan.name,
                    "attended" to true,
                    "streakCount" to newStreak,
                    "isAdditional" to plan.isAdditional
                )
            )
    }

    suspend fun clearUserData() {
        val userId = currentUserId
        if (userId.isEmpty()) return

        dao.clearAllWorkoutPlans(userId)
        dao.clearAllTrainingLogs(userId)
        dao.clearAllProgressEntries(userId)

        runCatching {
            val userRef = db.collection("users").document(userId)
            for (collectionName in listOf("workout_plans", "training_logs", "progress_entries")) {
                val snapshot = userRef.collection(collectionName).get().await()
                snapshot.documents.forEach { it.reference.delete() }
            }
            userRef.delete().await()
            storage.reference.child("profile_images/$userId.jpg").delete().await()
        }
    }

    private suspend fun cleanupOldAdditionalPlans(userId: String) {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        dao.deleteOldAdditionalPlans(userId, cal.timeInMillis)
    }

    private suspend fun syncWorkoutPlans(userId: String) {
        val plansSnapshot = db.collection("users").document(userId)
            .collection("workout_plans").get().await()
        val localPlans = dao.getAllWorkoutPlansList(userId)
        val cloudPlanNames = plansSnapshot.documents.mapNotNull { it.getString("name") }.toSet()

        localPlans.filterNot { it.name in cloudPlanNames }
            .forEach { dao.deleteWorkoutPlan(it) }

        for (doc in plansSnapshot.documents) {
            val name = doc.getString("name") ?: continue
            if (localPlans.none { it.name == name }) {
                dao.insertWorkoutPlan(
                    WorkoutPlan(
                        name = name,
                        days = doc.getString("days") ?: "",
                        muscleGroups = doc.getString("muscleGroups") ?: "",
                        hour = doc.getString("hour") ?: "08:00",
                        isAdditional = doc.getBoolean("isAdditional") ?: false,
                        userId = userId
                    )
                )
            }
        }
    }

    private suspend fun syncTrainingLogs(userId: String) {
        val logsSnapshot = db.collection("users").document(userId)
            .collection("training_logs").get().await()
        val updatedPlans = dao.getAllWorkoutPlansList(userId)
        val localLogs = dao.getAllTrainingLogsList(userId)
        val cloudLogDates = logsSnapshot.documents.mapNotNull { it.getLong("date") }.toSet()

        localLogs.filterNot { it.date in cloudLogDates }
            .forEach { dao.deleteTrainingLog(it) }

        for (doc in logsSnapshot.documents) {
            val date = doc.getLong("date") ?: continue
            val planName = doc.getString("planName") ?: ""
            val plan = updatedPlans.find { it.name == planName } ?: continue
            if (localLogs.none { it.date == date && it.workoutPlanId == plan.id }) {
                dao.insertTrainingLog(
                    TrainingLog(
                        date = date,
                        attended = doc.getBoolean("attended") ?: true,
                        workoutPlanId = plan.id,
                        streakCount = doc.getLong("streakCount")?.toInt() ?: 1,
                        isAdditional = doc.getBoolean("isAdditional") ?: false,
                        userId = userId
                    )
                )
            }
        }
    }

    private suspend fun syncProgressEntries(userId: String) {
        val progressSnapshot = db.collection("users").document(userId)
            .collection("progress_entries").get().await()
        val currentLocalProgress = dao.getAllProgressEntriesList(userId)
        val cloudProgressDates = progressSnapshot.documents.mapNotNull { it.getLong("date") }.toSet()

        currentLocalProgress.filterNot { it.date in cloudProgressDates }
            .forEach { dao.deleteProgressEntry(it) }

        for (doc in progressSnapshot.documents) {
            val date = doc.getLong("date") ?: continue
            val weight = doc.getDouble("weightKg")?.toFloat() ?: 0f
            val alreadyExists = currentLocalProgress.any { it.date == date && it.weightKg == weight }
            if (!alreadyExists) {
                dao.insertProgressEntry(
                    ProgressEntry(
                        date = date,
                        weightKg = weight,
                        bodyFatPercentage = doc.getDouble("bodyFatPercentage")?.toFloat(),
                        photoUri = doc.getString("photoUri"),
                        notes = doc.getString("notes"),
                        userId = userId
                    )
                )
            }
        }
    }

    private fun checkMissedConstantWorkouts(
        start: Calendar,
        end: Calendar,
        plans: List<WorkoutPlan>,
        logs: List<TrainingLog>
    ): Boolean {
        val check = (start.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            resetTime(this)
        }
        val endLimit = (end.clone() as Calendar).apply { resetTime(this) }
        val dayNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

        while (check.before(endLimit)) {
            val dayName = dayNames[check.get(Calendar.DAY_OF_WEEK) - 1]
            val scheduled = plans.filter { !it.isAdditional && it.days.contains(dayName) }
            for (plan in scheduled) {
                if (logs.none { it.workoutPlanId == plan.id && isSameDay(it.date, check.timeInMillis) }) {
                    return true
                }
            }
            check.add(Calendar.DAY_OF_YEAR, 1)
        }
        return false
    }

    private fun resetTime(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private fun isSameDay(d1: Long, d2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = d1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = d2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }
}
