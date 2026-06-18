package com.example.gymbuddy.ui.screens

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gymbuddy.data.TrainingLog
import com.example.gymbuddy.data.WorkoutPlan
import com.example.gymbuddy.ui.AdditionalWorkoutColor
import com.example.gymbuddy.ui.AppBackground
import com.example.gymbuddy.ui.ScreenCard
import com.example.gymbuddy.ui.SectionTitle

@Composable
fun WorkoutPlansScreen(
    plans: List<WorkoutPlan>,
    logs: List<TrainingLog>,
    onMarkDone: (WorkoutPlan, Boolean) -> Unit,
    onDeletePlan: (WorkoutPlan) -> Unit
) {
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
                WorkoutPlanCard(plan, logs, onMarkDone = { onMarkDone(plan, true) }, onDeletePlan = { onDeletePlan(plan) })
            }

            item {
                SectionTitle(
                    text = "Treinos Adicionais (Ocasionais)",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (additionalPlans.isEmpty()) {
                item {
                    Text("Sem treinos adicionais esta semana.", modifier = Modifier.padding(horizontal = 16.dp))
                }
            } else {
                items(additionalPlans, key = { it.id }) { plan ->
                    WorkoutPlanCard(plan, logs, onMarkDone = { onMarkDone(plan, false) }, onDeletePlan = { onDeletePlan(plan) })
                }
            }

            if (plans.isEmpty()) {
                item {
                    Text("Nenhum plano de treino definido.", modifier = Modifier.padding(16.dp))
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
    val isDoneThisWeek = logs.any { it.workoutPlanId == plan.id && isWithinCurrentWeek(it.date) }

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
