package com.example.gymbuddy.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.gymbuddy.data.ProgressEntry
import com.example.gymbuddy.ui.AppBackground
import com.example.gymbuddy.ui.AppTextField

@Composable
fun AddProgressScreen(
    onSave: (ProgressEntry) -> Unit,
    onInvalidWeight: () -> Unit
) {
    val context = LocalContext.current
    var weight by remember { mutableStateOf("") }
    var bodyFat by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val selectedPhotoUris = remember { mutableStateListOf<Uri>() }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            if (!selectedPhotoUris.contains(uri)) {
                selectedPhotoUris.add(uri)
                try {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppTextField(value = weight, onValueChange = { weight = it }, label = "Peso (kg)")
        AppTextField(value = bodyFat, onValueChange = { bodyFat = it }, label = "Gordura Corporal (%)")
        AppTextField(value = notes, onValueChange = { notes = it }, label = "Notas", singleLine = false)

        Text("FOTOS DE PROGRESSO", style = MaterialTheme.typography.titleMedium)
        if (selectedPhotoUris.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selectedPhotoUris) { uri ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp))
                        )
                        TextButton(onClick = { selectedPhotoUris.remove(uri) }) {
                            Text("Remover")
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = { pickerLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
            Text("ADICIONAR FOTOS")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val parsedWeight = weight.toFloatOrNull()
                if (parsedWeight == null) {
                    onInvalidWeight()
                } else {
                    onSave(
                        ProgressEntry(
                            date = System.currentTimeMillis(),
                            weightKg = parsedWeight,
                            bodyFatPercentage = bodyFat.toFloatOrNull(),
                            photoUri = selectedPhotoUris.joinToString(","),
                            notes = notes.takeIf { it.isNotBlank() }
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("SALVAR PROGRESSO")
        }
        Spacer(modifier = Modifier.height(88.dp))
    }
}
