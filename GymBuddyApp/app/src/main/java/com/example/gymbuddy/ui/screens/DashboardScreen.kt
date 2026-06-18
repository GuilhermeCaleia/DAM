package com.example.gymbuddy.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.gymbuddy.R
import com.example.gymbuddy.data.TrainingLog
import com.example.gymbuddy.data.WorkoutPlan
import com.example.gymbuddy.ui.AppBackground
import com.example.gymbuddy.ui.DayStatusDot
import com.example.gymbuddy.ui.ScreenCard

@Composable
fun DashboardScreen(
    userFirstName: String,
    workoutPlans: List<WorkoutPlan>,
    trainingLogs: List<TrainingLog>
) {
    val streak = trainingLogs.firstOrNull()?.streakCount ?: 0
    val nextWorkout = calculateNextWorkout(workoutPlans, trainingLogs)
    val dots = calculateWeekDots(workoutPlans, trainingLogs)

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
                text = "Olá $userFirstName",
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
            Text(currentWeekRange(), color = MaterialTheme.colorScheme.primary)
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
    logs: List<TrainingLog>
): List<Pair<Color, String>> {
    val labels = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val result = mutableListOf<Pair<Color, String>>()

    val cal = java.util.Calendar.getInstance()
    cal.firstDayOfWeek = java.util.Calendar.MONDAY
    while (cal.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.MONDAY) {
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
    }
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)

    for (i in 0 until 7) {
        val dayStart = cal.timeInMillis
        val nextDayCal = (cal.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, 1) }
        val dayEnd = nextDayCal.timeInMillis
        val dayLogs = logs.filter { it.date in dayStart until dayEnd }
        val isScheduled = plans.any { !it.isAdditional && it.days.contains(dayNames[i]) }
        val color = when {
            dayLogs.any { it.attended } -> Color(0xFF4CAF50)
            isScheduled -> Color(0xFFF44336)
            else -> Color(0xFFBDBDBD)
        }
        result += color to labels[i]
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
    }

    return result
}
