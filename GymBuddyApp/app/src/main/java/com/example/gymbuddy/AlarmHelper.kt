package com.example.gymbuddy

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.gymbuddy.data.WorkoutPlan
import java.util.*

class AlarmHelper(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleWorkoutAlarms(plans: List<WorkoutPlan>) {
        val dayMap = mapOf(
            "Monday" to Calendar.MONDAY,
            "Tuesday" to Calendar.TUESDAY,
            "Wednesday" to Calendar.WEDNESDAY,
            "Thursday" to Calendar.THURSDAY,
            "Friday" to Calendar.FRIDAY,
            "Saturday" to Calendar.SATURDAY,
            "Sunday" to Calendar.SUNDAY
        )

        for (plan in plans) {
            val planDays = plan.days.split(", ")
            val timeParts = plan.hour.split(":")
            if (timeParts.size != 2) continue

            for (dayStr in planDays) {
                val dayInt = dayMap[dayStr] ?: continue
                
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, dayInt)
                    set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    set(Calendar.MINUTE, timeParts[1].toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    
                    // Move to 4 hours before
                    add(Calendar.HOUR_OF_DAY, -4)
                    
                    // If the calculated time is in the past for this week, move to next week
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }

                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("plan_name", plan.name)
                    putExtra("plan_hour", plan.hour)
                    putExtra("notif_id", (plan.id * 7 + dayInt).toInt())
                }

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    (plan.id * 7 + dayInt).toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                scheduleExactAlarm(calendar.timeInMillis, pendingIntent)
            }
        }
    }

    private fun scheduleExactAlarm(timeInMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } else {
                // Fallback to inexact if permission missing
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
    }
}
