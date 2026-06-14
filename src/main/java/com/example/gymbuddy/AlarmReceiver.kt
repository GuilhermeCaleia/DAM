package com.example.gymbuddy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val planName = intent.getStringExtra("plan_name") ?: "Treino"
        val planHour = intent.getStringExtra("plan_hour") ?: ""
        val notificationId = intent.getIntOfExtra("notif_id", 200)

        val helper = NotificationHelper(context)
        helper.sendNotification(
            notificationId,
            "Treino à vista!",
            "Tens treino de $planName hoje às $planHour. Não desistas!"
        )
    }

    private fun Intent.getIntOfExtra(name: String, defaultValue: Int): Int {
        return if (hasExtra(name)) getIntExtra(name, defaultValue) else defaultValue
    }
}
