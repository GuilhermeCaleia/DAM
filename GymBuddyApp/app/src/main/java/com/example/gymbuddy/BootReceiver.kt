package com.example.gymbuddy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.gymbuddy.data.AppDatabase
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val userId = Firebase.auth.currentUser?.uid
            if (userId != null) {
                // Use a coroutine to fetch plans from DB and reschedule
                val dao = AppDatabase.getDatabase(context).gymDao()
                CoroutineScope(Dispatchers.IO).launch {
                    val plans = dao.getAllWorkoutPlansList(userId)
                    AlarmHelper(context).scheduleWorkoutAlarms(plans)
                }
            }
        }
    }
}
