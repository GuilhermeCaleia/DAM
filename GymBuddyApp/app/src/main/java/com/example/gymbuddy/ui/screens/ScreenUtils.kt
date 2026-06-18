package com.example.gymbuddy.ui.screens

import com.example.gymbuddy.data.TrainingLog
import com.example.gymbuddy.data.WorkoutPlan
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun currentWeekRange(): String {
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }

    val startDay = cal.get(Calendar.DAY_OF_MONTH)
    val locale = Locale.forLanguageTag("pt-PT")
    val startMonth = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, locale)

    val calEnd = cal.clone() as Calendar
    calEnd.add(Calendar.DAY_OF_YEAR, 6)
    val endDay = calEnd.get(Calendar.DAY_OF_MONTH)
    val endMonth = calEnd.getDisplayName(Calendar.MONTH, Calendar.LONG, locale)

    return if (startMonth == endMonth) "$startDay-$endDay $startMonth" else "$startDay $startMonth - $endDay $endMonth"
}

fun formatDays(days: String): String = days.split(", ").joinToString(", ") {
    when (it) {
        "Monday" -> "Seg"
        "Tuesday" -> "Ter"
        "Wednesday" -> "Qua"
        "Thursday" -> "Qui"
        "Friday" -> "Sex"
        "Saturday" -> "Sáb"
        "Sunday" -> "Dom"
        else -> it
    }
}

fun isWithinCurrentWeek(time: Long): Boolean {
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val start = cal.timeInMillis

    val calEnd = cal.clone() as Calendar
    calEnd.add(Calendar.DAY_OF_YEAR, 7)
    val end = calEnd.timeInMillis

    return time in start until end
}

fun isSameDay(date1: Long, date2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun calculateTargetDate(plan: WorkoutPlan): Long {
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    val dayMap = mapOf(
        "Monday" to 0, "Tuesday" to 1, "Wednesday" to 2, "Thursday" to 3,
        "Friday" to 4, "Saturday" to 5, "Sunday" to 6
    )

    val planDays = plan.days.split(", ")
    val currentDayName = SimpleDateFormat("EEEE", Locale.ENGLISH).format(now.time)

    val targetDayName = if (planDays.contains(currentDayName)) {
        currentDayName
    } else {
        val daysInPast = planDays.filter { dayName ->
            val targetDayIdx = dayMap[dayName] ?: 0
            val currentDayIdx = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7
            targetDayIdx <= currentDayIdx
        }.sortedByDescending { dayMap[it] }

        daysInPast.firstOrNull() ?: planDays.first()
    }

    val offset = dayMap[targetDayName] ?: 0
    cal.add(Calendar.DAY_OF_YEAR, offset)

    val timeParts = plan.hour.split(":")
    if (timeParts.size == 2) {
        cal.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
        cal.set(Calendar.MINUTE, timeParts[1].toInt())
    }

    return cal.timeInMillis
}

fun calculateNextWorkout(
    plans: List<WorkoutPlan>,
    logs: List<TrainingLog>
): Pair<String, String>? {
    if (plans.isEmpty()) return null

    val now = Calendar.getInstance()
    val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
    val dayMap = mapOf(
        "Monday" to Calendar.MONDAY,
        "Tuesday" to Calendar.TUESDAY,
        "Wednesday" to Calendar.WEDNESDAY,
        "Thursday" to Calendar.THURSDAY,
        "Friday" to Calendar.FRIDAY,
        "Saturday" to Calendar.SATURDAY,
        "Sunday" to Calendar.SUNDAY
    )

    var bestPlan: WorkoutPlan? = null
    var minDiff = Int.MAX_VALUE
    var bestDate: Calendar? = null

    for (plan in plans) {
        for (dayStr in plan.days.split(", ")) {
            val dayInt = dayMap[dayStr] ?: continue
            var diff = dayInt - currentDayOfWeek
            if (diff < 0) diff += 7

            if (diff == 0) {
                val timeParts = plan.hour.split(":")
                val planTimeToday = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    set(Calendar.MINUTE, timeParts[1].toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (now.after(planTimeToday)) diff = 7
            }

            val checkDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, diff)
                val timeParts = plan.hour.split(":")
                set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(Calendar.MINUTE, timeParts[1].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val isAlreadyDone = logs.any { it.workoutPlanId == plan.id && isSameDay(it.date, checkDate.timeInMillis) }
            var finalDiff = diff
            var finalCheckDate = checkDate
            if (isAlreadyDone) {
                finalDiff += 7
                finalCheckDate = (checkDate.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 7) }
            }

            if (finalDiff < minDiff) {
                minDiff = finalDiff
                bestPlan = plan
                bestDate = finalCheckDate
            }
        }
    }

    if (bestPlan == null || bestDate == null) return null
    val sdf = SimpleDateFormat("EEE, dd MMM • HH:mm", Locale.getDefault())
    return bestPlan.muscleGroups to sdf.format(bestDate.time).replaceFirstChar { it.uppercase() }
}
