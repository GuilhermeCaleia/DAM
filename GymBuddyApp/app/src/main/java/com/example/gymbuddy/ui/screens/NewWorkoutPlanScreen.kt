package com.example.gymbuddy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gymbuddy.data.WorkoutPlan
import com.example.gymbuddy.ui.AppBackground
import com.example.gymbuddy.ui.AppTextField
import com.example.gymbuddy.ui.ChoiceChip
import com.example.gymbuddy.ui.ToggleRow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewWorkoutPlanScreen(
    onSave: (WorkoutPlan) -> Unit,
    onInvalidForm: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedHour by remember { mutableStateOf("08:00") }
    var isAdditional by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val selectedDays = remember { mutableStateListOf<String>() }
    val selectedMuscles = remember { mutableStateListOf<String>() }
    val allDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    val allMuscles = listOf("Peito", "Costas", "Pernas", "Bicep", "Tricep")

    if (showTimePicker) {
        ComposeTimePickerDialog(
            initialTime = selectedHour,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                selectedHour = it
                showTimePicker = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppTextField(value = name, onValueChange = { name = it }, label = "Nome do Plano")
        Text("DIAS DO TREINO", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            allDays.forEach { day ->
                ChoiceChip(
                    selected = selectedDays.contains(day),
                    onClick = {
                        if (selectedDays.contains(day)) selectedDays.remove(day) else selectedDays.add(day)
                    },
                    label = day
                )
            }
        }

        Text("GRUPOS MUSCULARES", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            allMuscles.forEach { muscle ->
                ChoiceChip(
                    selected = selectedMuscles.contains(muscle),
                    onClick = {
                        if (selectedMuscles.contains(muscle)) selectedMuscles.remove(muscle) else selectedMuscles.add(muscle)
                    },
                    label = muscle
                )
            }
        }

        OutlinedButton(
            onClick = { showTimePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Hora: $selectedHour")
        }
        ToggleRow(label = "TREINO OCASIONAL", checked = isAdditional, onCheckedChange = { isAdditional = it })
        AppTextField(value = notes, onValueChange = { notes = it }, label = "Notas (Opcional)", singleLine = false)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (name.isBlank() || selectedDays.isEmpty() || selectedMuscles.isEmpty()) {
                    onInvalidForm()
                } else {
                    onSave(
                        WorkoutPlan(
                            name = name,
                            days = selectedDays.joinToString(", "),
                            muscleGroups = selectedMuscles.joinToString(", "),
                            hour = selectedHour,
                            isAdditional = isAdditional,
                            notes = notes.takeIf { it.isNotBlank() }
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("GUARDAR PLANO")
        }
        Spacer(modifier = Modifier.height(88.dp))
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ComposeTimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecione a Hora") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(String.format("%02d:%02d", state.hour, state.minute)) }) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
