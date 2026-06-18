package com.example.gymbuddy

import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.gymbuddy.data.WorkoutPlan
import com.example.gymbuddy.ui.AppBackground
import com.example.gymbuddy.ui.DayStatusDot
import com.example.gymbuddy.ui.GymBuddyTheme
import com.example.gymbuddy.ui.ScreenCard
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {

    private val viewModel: GymViewModel by viewModels()

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            GymBuddyTheme {
                DashboardScreen(viewModel)
            }
        }
    }
}

@Composable
private fun DashboardScreen(viewModel: GymViewModel) {
    val logs by viewModel.trainingLogs.observeAsState(emptyList())
    val plans by viewModel.workoutPlans.observeAsState(emptyList())
    val streak = logs.firstOrNull()?.streakCount ?: 0
    val fullName = Firebase.auth.currentUser?.displayName ?: "Utilizador"
    val firstName = fullName.split(" ").firstOrNull() ?: fullName
    val nextWorkout = calculateNextWorkout(plans, logs)
    val dots = calculateWeekDots(plans, logs)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Olá $firstName",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            Image(
                painter = painterResource(id = R.drawable.gymbuddy),
                contentDescription = "GymBuddy Logo",
                modifier = Modifier.size(142.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        ScreenCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("$streak dias seguidos!", style = MaterialTheme.typography.headlineSmall)
                Text("Continua assim, não pares agora", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Próximo treino", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        ScreenCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(16.dp)
            ) {
                Text(
                    text = nextWorkout?.first ?: "Nenhum plano definido",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = nextWorkout?.second ?: "-",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Esta Semana", style = MaterialTheme.typography.titleMedium)
            Text(dashboardWeekRange(), color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dots.forEach { (color, label) ->
                DayStatusDot(color = color, label = label)
            }
        }
        Spacer(modifier = Modifier.height(88.dp))
    }
}

private fun calculateWeekDots(
    plans: List<WorkoutPlan>,
    logs: List<com.example.gymbuddy.data.TrainingLog>
): List<Pair<Color, String>> {
    val labels = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val result = mutableListOf<Pair<Color, String>>()

    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)

    for (i in 0 until 7) {
        val dayStart = cal.timeInMillis
        val nextDayCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val dayEnd = nextDayCal.timeInMillis
        val dayLogs = logs.filter { it.date in dayStart until dayEnd }
        val isScheduled = plans.any { !it.isAdditional && it.days.contains(dayNames[i]) }
        val color = when {
            dayLogs.any { it.attended } -> Color(0xFF4CAF50)
            isScheduled -> Color(0xFFF44336)
            else -> Color(0xFFBDBDBD)
        }
        result += color to labels[i]
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return result
}

private fun calculateNextWorkout(
    plans: List<WorkoutPlan>,
    logs: List<com.example.gymbuddy.data.TrainingLog>
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

private fun isSameDay(date1: Long, date2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun dashboardWeekRange(): String {
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }

    val startDay = cal.get(Calendar.DAY_OF_MONTH)
    val startMonth = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "PT"))

    val calEnd = cal.clone() as Calendar
    calEnd.add(Calendar.DAY_OF_YEAR, 6)
    val endDay = calEnd.get(Calendar.DAY_OF_MONTH)
    val endMonth = calEnd.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "PT"))

    return if (startMonth == endMonth) "$startDay-$endDay $startMonth" else "$startDay $startMonth - $endDay $endMonth"
}
