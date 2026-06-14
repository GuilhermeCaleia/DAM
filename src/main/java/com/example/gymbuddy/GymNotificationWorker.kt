package com.example.gymbuddy

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.example.gymbuddy.data.AppDatabase
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.Calendar

class GymNotificationWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val helper = NotificationHelper(context)
    private val dao = AppDatabase.getDatabase(context).gymDao()
    private val prefs = context.getSharedPreferences("gym_notif_prefs", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result {
        // We get the UID again to ensure we have the latest session
        val currentUserId = Firebase.auth.currentUser?.uid ?: return Result.success()

        // Clean up old keys once a day to save memory
        cleanupOldPrefs()

        checkMonthlyPhotoReminder()
        checkLastWorkoutInactivity(currentUserId)
        checkUpcomingWorkout(currentUserId)

        return Result.success()
    }

    private fun checkMonthlyPhotoReminder() {
        val cal = Calendar.getInstance()
        val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (cal.get(Calendar.DAY_OF_MONTH) == lastDay) {
            val key = "photo_notif_${cal.get(Calendar.MONTH)}_${cal.get(Calendar.YEAR)}"
            if (!prefs.getBoolean(key, false)) {
                helper.sendNotification(101, "Hora da Foto!", "É o último dia do mês. Tira uma foto de progresso!")
                prefs.edit().putBoolean(key, true).apply()
            }
        }
    }

    private suspend fun checkLastWorkoutInactivity(uid: String) {
        val lastLogs = dao.getAllTrainingLogsList(uid)
        if (lastLogs.isNotEmpty()) {
            val lastWorkoutTime = lastLogs.first().date
            val oneWeekMillis = 7 * 24 * 60 * 60 * 1000L
            val now = System.currentTimeMillis()
            
            if (now - lastWorkoutTime > oneWeekMillis) {
                val lastInactivityNotif = prefs.getLong("last_inactivity_notif", 0L)
                // Once every 3 days max
                if (now - lastInactivityNotif > 3 * 24 * 60 * 60 * 1000L) {
                    helper.sendNotification(102, "Sentimos a tua falta", "Já não te vemos há um tempo. Vamos ao ginásio?")
                    prefs.edit().putLong("last_inactivity_notif", now).apply()
                }
            }
        }
    }

    private suspend fun checkUpcomingWorkout(uid: String) {
        val plans = dao.getAllWorkoutPlansList(uid)
        val now = Calendar.getInstance()
        
        // Reset seconds/millis for cleaner calculation
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)

        val dayNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val currentDayName = dayNames[now.get(Calendar.DAY_OF_WEEK) - 1]

        for (plan in plans) {
            if (plan.days.contains(currentDayName)) {
                val timeParts = plan.hour.split(":")
                if (timeParts.size == 2) {
                    val workoutCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                        set(Calendar.MINUTE, timeParts[1].toInt())
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    
                    val diffMillis = workoutCal.timeInMillis - now.timeInMillis
                    
                    // IF workout is today AND is in the future AND is within the next 4h 30m
                    // We use a window to ensure PeriodicWork (which is not exact) hits it.
                    if (diffMillis > 0 && diffMillis <= 4.5 * 60 * 60 * 1000L) {
                        val todayKey = "workout_${plan.id}_${now.get(Calendar.DAY_OF_YEAR)}_${now.get(Calendar.YEAR)}"
                        if (!prefs.getBoolean(todayKey, false)) {
                            helper.sendNotification(
                                200 + plan.id.toInt(), 
                                "Treino à vista!", 
                                "Tens treino de ${plan.name} hoje às ${plan.hour}. Não desistas!"
                            )
                            prefs.edit().putBoolean(todayKey, true).apply()
                        }
                    }
                }
            }
        }
    }

    private fun cleanupOldPrefs() {
        // Optional: logic to clear prefs older than 7 days
    }
}
