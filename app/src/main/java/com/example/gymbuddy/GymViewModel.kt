package com.example.gymbuddy

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.gymbuddy.data.AppDatabase
import com.example.gymbuddy.data.ProgressEntry
import com.example.gymbuddy.data.TrainingLog
import com.example.gymbuddy.data.WorkoutPlan
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class GymViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).gymDao()
    private val auth = Firebase.auth
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private val userId: String
        get() = auth.currentUser?.uid ?: ""

    val workoutPlans = dao.getAllWorkoutPlans(userId).asLiveData()
    val trainingLogs = dao.getAllTrainingLogs(userId).asLiveData()
    val progressEntries = dao.getAllProgressEntries(userId).asLiveData()

    private var isSyncing = false

    init {
        refreshSync()
    }

    fun refreshSync() {
        if (userId.isEmpty() || isSyncing) return
        cleanupOldAdditionalPlans()
        syncWithFirestore()
    }

    private fun syncWithFirestore() = viewModelScope.launch {
        isSyncing = true
        val currentUid = userId
        
        try {
            // 1. Sincronizar Planos de Treino
            val plansSnapshot = db.collection("users").document(currentUid)
                .collection("workout_plans").get().await()
            val localPlans = dao.getAllWorkoutPlansList(currentUid)
            val cloudPlanNames = plansSnapshot.documents.mapNotNull { it.getString("name") }.toSet()
            
            for (localPlan in localPlans) {
                if (!cloudPlanNames.contains(localPlan.name)) {
                    dao.deleteWorkoutPlan(localPlan)
                }
            }
            
            for (doc in plansSnapshot.documents) {
                val name = doc.getString("name") ?: continue
                if (!localPlans.any { it.name == name }) {
                    dao.insertWorkoutPlan(WorkoutPlan(
                        name = name,
                        days = doc.getString("days") ?: "",
                        muscleGroups = doc.getString("muscleGroups") ?: "",
                        hour = doc.getString("hour") ?: "08:00",
                        isAdditional = doc.getBoolean("isAdditional") ?: false,
                        userId = currentUid
                    ))
                }
            }

            // 2. Sincronizar Logs de Treino
            val logsSnapshot = db.collection("users").document(currentUid)
                .collection("training_logs").get().await()
            val updatedPlans = dao.getAllWorkoutPlansList(currentUid)
            val localLogs = dao.getAllTrainingLogsList(currentUid)
            val cloudLogDates = logsSnapshot.documents.mapNotNull { it.getLong("date") }.toSet()

            for (localLog in localLogs) {
                if (!cloudLogDates.contains(localLog.date)) {
                    dao.deleteTrainingLog(localLog)
                }
            }

            for (doc in logsSnapshot.documents) {
                val date = doc.getLong("date") ?: continue
                val planName = doc.getString("planName") ?: ""
                val plan = updatedPlans.find { it.name == planName }
                if (plan != null && !localLogs.any { it.date == date && it.workoutPlanId == plan.id }) {
                    dao.insertTrainingLog(TrainingLog(
                        date = date,
                        attended = doc.getBoolean("attended") ?: true,
                        workoutPlanId = plan.id,
                        streakCount = doc.getLong("streakCount")?.toInt() ?: 1,
                        isAdditional = doc.getBoolean("isAdditional") ?: false,
                        userId = currentUid
                    ))
                }
            }

            // 3. Sincronizar Progresso (REFORÇADO CONTRA DUPLICADOS)
            val progressSnapshot = db.collection("users").document(currentUid)
                .collection("progress_entries").get().await()
            
            // Recarregar sempre a lista local antes de comparar para evitar duplicados em massa
            val currentLocalProgress = dao.getAllProgressEntriesList(currentUid)
            val cloudProgressDates = progressSnapshot.documents.mapNotNull { it.getLong("date") }.toSet()
            
            for (localEntry in currentLocalProgress) {
                if (!cloudProgressDates.contains(localEntry.date)) {
                    dao.deleteProgressEntry(localEntry)
                }
            }

            for (doc in progressSnapshot.documents) {
                val date = doc.getLong("date") ?: continue
                val weight = doc.getDouble("weightKg")?.toFloat() ?: 0f
                
                // Verificação dupla: Data e Peso
                val alreadyExists = currentLocalProgress.any { it.date == date && it.weightKg == weight }
                
                if (!alreadyExists) {
                    dao.insertProgressEntry(ProgressEntry(
                        date = date,
                        weightKg = weight,
                        bodyFatPercentage = doc.getDouble("bodyFatPercentage")?.toFloat(),
                        photoUri = doc.getString("photoUri"),
                        notes = doc.getString("notes"),
                        userId = currentUid
                    ))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isSyncing = false
        }
    }

    private fun cleanupOldAdditionalPlans() = viewModelScope.launch {
        val currentUid = userId
        if (currentUid.isEmpty()) return@launch
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        dao.deleteOldAdditionalPlans(currentUid, cal.timeInMillis)
    }

    fun insertWorkoutPlan(plan: WorkoutPlan) = viewModelScope.launch {
        val currentUid = userId
        if (currentUid.isEmpty()) return@launch
        dao.insertWorkoutPlan(plan.copy(userId = currentUid))
        db.collection("users").document(currentUid).collection("workout_plans").document(plan.name)
            .set(hashMapOf("name" to plan.name, "days" to plan.days, "muscleGroups" to plan.muscleGroups, "hour" to plan.hour, "isAdditional" to plan.isAdditional))
    }

    fun deleteWorkoutPlan(plan: WorkoutPlan) = viewModelScope.launch {
        val currentUid = userId
        if (currentUid.isEmpty()) return@launch
        dao.deleteWorkoutPlan(plan)
        try {
            db.collection("users").document(currentUid)
                .collection("workout_plans").document(plan.name)
                .delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun insertProgressEntry(entry: ProgressEntry) = viewModelScope.launch {
        val currentUid = userId
        if (currentUid.isEmpty()) return@launch
        
        val cloudUrls = mutableListOf<String>()
        val localUris = entry.photoUri?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

        for (localUri in localUris) {
            if (localUri.startsWith("content://")) {
                try {
                    val timestamp = System.currentTimeMillis()
                    val ref = storage.reference.child("progress_images/$currentUid/${entry.date}_$timestamp.jpg")
                    ref.putFile(Uri.parse(localUri)).await()
                    cloudUrls.add(ref.downloadUrl.await().toString())
                } catch (e: Exception) { e.printStackTrace() }
            } else {
                cloudUrls.add(localUri)
            }
        }

        val cloudUrlsString = if (cloudUrls.isNotEmpty()) cloudUrls.joinToString(",") else null
        val finalEntry = entry.copy(userId = currentUid, photoUri = cloudUrlsString)
        dao.insertProgressEntry(finalEntry)
        
        db.collection("users").document(currentUid).collection("progress_entries").document(entry.date.toString())
            .set(hashMapOf(
                "date" to entry.date,
                "weightKg" to entry.weightKg,
                "bodyFatPercentage" to entry.bodyFatPercentage,
                "photoUri" to cloudUrlsString,
                "notes" to entry.notes
            ))
    }

    fun deleteProgressEntry(entry: ProgressEntry) = viewModelScope.launch {
        val currentUid = userId
        if (currentUid.isEmpty()) return@launch
        dao.deleteProgressEntry(entry)
        try {
            db.collection("users").document(currentUid)
                .collection("progress_entries").document(entry.date.toString())
                .delete().await()
            entry.photoUri?.let { uri ->
                if (uri.contains("firebaseapp.com") || uri.contains("googleapis.com")) {
                    storage.getReferenceFromUrl(uri).delete().await()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun markAttendance(plan: WorkoutPlan, dateOverride: Long? = null) = viewModelScope.launch {
        val currentUid = userId
        if (currentUid.isEmpty()) return@launch
        val targetDate = dateOverride ?: System.currentTimeMillis()
        val allLogs = dao.getAllTrainingLogsList(currentUid)
        if (allLogs.any { it.workoutPlanId == plan.id && isSameDay(it.date, targetDate) }) return@launch

        val lastLog = allLogs.firstOrNull()
        var newStreak = 1
        if (lastLog != null) {
            val missed = checkMissedConstantWorkouts(Calendar.getInstance().apply { timeInMillis = lastLog.date }, Calendar.getInstance().apply { timeInMillis = targetDate }, dao.getAllWorkoutPlansList(currentUid), allLogs)
            newStreak = if (missed) 1 else lastLog.streakCount + 1
        }

        dao.insertTrainingLog(TrainingLog(
            date = targetDate, 
            attended = true, 
            workoutPlanId = plan.id, 
            streakCount = newStreak, 
            isAdditional = plan.isAdditional, 
            userId = currentUid
        ))
        db.collection("users").document(currentUid).collection("training_logs").document(targetDate.toString())
            .set(hashMapOf("date" to targetDate, "planName" to plan.name, "attended" to true, "streakCount" to newStreak, "isAdditional" to plan.isAdditional))
    }

    private fun checkMissedConstantWorkouts(start: Calendar, end: Calendar, plans: List<WorkoutPlan>, logs: List<TrainingLog>): Boolean {
        val check = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1); resetTime(this) }
        val endLimit = (end.clone() as Calendar).apply { resetTime(this) }
        val dayNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        while (check.before(endLimit)) {
            val dayName = dayNames[check.get(Calendar.DAY_OF_WEEK) - 1]
            val scheduled = plans.filter { !it.isAdditional && it.days.contains(dayName) }
            for (p in scheduled) if (!logs.any { it.workoutPlanId == p.id && isSameDay(it.date, check.timeInMillis) }) return true
            check.add(Calendar.DAY_OF_YEAR, 1)
        }
        return false
    }

    private fun resetTime(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    }

    fun clearUserData() = viewModelScope.launch {
        val currentUid = userId
        if (currentUid.isEmpty()) return@launch
        dao.clearAllWorkoutPlans(currentUid); dao.clearAllTrainingLogs(currentUid); dao.clearAllProgressEntries(currentUid)
        try {
            val userRef = db.collection("users").document(currentUid)
            for (coll in listOf("workout_plans", "training_logs", "progress_entries")) {
                val snapshot = userRef.collection(coll).get().await()
                for (doc in snapshot.documents) doc.reference.delete()
            }
            userRef.delete().await()
            storage.reference.child("profile_images/$currentUid.jpg").delete().await()
        } catch (e: Exception) {}
    }

    private fun isSameDay(d1: Long, d2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = d1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = d2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }
}
