package com.example.gymbuddy

import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.gymbuddy.data.TrainingLog
import com.example.gymbuddy.data.WorkoutPlan
import com.example.gymbuddy.ui.AdditionalWorkoutColor
import com.example.gymbuddy.ui.AppBackground
import com.example.gymbuddy.ui.GymBuddyTheme
import com.example.gymbuddy.ui.ScreenCard
import com.example.gymbuddy.ui.SectionTitle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FirstFragment : Fragment() {

    private val viewModel: GymViewModel by activityViewModels()

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            GymBuddyTheme {
                WorkoutPlansScreen(
                    viewModel = viewModel,
                    onMarkDone = { plan, isRegular ->
                        if (isRegular) {
                            viewModel.markAttendance(plan, calculateTargetDate(plan))
                            Toast.makeText(requireContext(), "Treino concluído!", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.markAttendance(plan)
                            Toast.makeText(requireContext(), "Treino adicional concluído!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeletePlan = { plan ->
                        viewModel.deleteWorkoutPlan(plan)
                        Toast.makeText(requireContext(), "Plano eliminado", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun calculateTargetDate(plan: WorkoutPlan): Long {
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
}

@Composable
private fun WorkoutPlansScreen(
    viewModel: GymViewModel,
    onMarkDone: (WorkoutPlan, Boolean) -> Unit,
    onDeletePlan: (WorkoutPlan) -> Unit
) {
    val plans by viewModel.workoutPlans.observeAsState(emptyList())
    val logs by viewModel.trainingLogs.observeAsState(emptyList())

    LaunchedEffect(Unit) {
        viewModel.refreshSync()
    }

    val regularPlans = plans.filter { !it.isAdditional }
    val additionalPlans = plans.filter { it.isAdditional }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Planos Constantes", style = MaterialTheme.typography.headlineSmall)
                    Text(currentWeekRange(), color = MaterialTheme.colorScheme.primary)
                }
            }

            items(regularPlans, key = { it.id }) { plan ->
                WorkoutPlanCard(plan = plan, logs = logs, onMarkDone = { onMarkDone(plan, true) }, onDeletePlan = { onDeletePlan(plan) })
            }

            item {
                SectionTitle(
                    text = "Treinos Adicionais (Ocasionais)",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (additionalPlans.isEmpty()) {
                item {
                    Text(
                        text = "Sem treinos adicionais esta semana.",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                items(additionalPlans, key = { it.id }) { plan ->
                    WorkoutPlanCard(plan = plan, logs = logs, onMarkDone = { onMarkDone(plan, false) }, onDeletePlan = { onDeletePlan(plan) })
                }
            }

            if (plans.isEmpty()) {
                item {
                    Text(
                        text = "Nenhum plano de treino definido.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(88.dp)) }
        }
    }
}

@Composable
private fun WorkoutPlanCard(
    plan: WorkoutPlan,
    logs: List<TrainingLog>,
    onMarkDone: () -> Unit,
    onDeletePlan: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isDoneThisWeek = logs.any { log -> log.workoutPlanId == plan.id && isWithinCurrentWeek(log.date) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Plano") },
            text = { Text("Queres eliminar o plano '${plan.name}'?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeletePlan()
                }) { Text("Sim") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Não") } }
        )
    }

    ScreenCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .combinedClickable(onClick = {}, onLongClick = { showDeleteDialog = true })
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (plan.isAdditional) AdditionalWorkoutColor else Color.Transparent)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (isDoneThisWeek) Color(0xFF4CAF50) else Color(0xFFF44336))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(plan.name, style = MaterialTheme.typography.titleMedium)
                Text(plan.muscleGroups, style = MaterialTheme.typography.bodyMedium)
                Text("${formatDays(plan.days)} às ${plan.hour}", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onMarkDone, enabled = !isDoneThisWeek) {
                Text(if (isDoneThisWeek) "Feito" else "Fiz")
            }
        }
    }
}

private fun formatDays(days: String): String = days.split(", ").joinToString(", ") {
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

private fun isWithinCurrentWeek(time: Long): Boolean {
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

private fun currentWeekRange(): String {
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
