package com.example.gymbuddy.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.gymbuddy.GymViewModel
import com.example.gymbuddy.NotificationHelper
import com.example.gymbuddy.R
import com.example.gymbuddy.ui.AppBackground
import com.example.gymbuddy.ui.AppTextField
import com.example.gymbuddy.ui.PhotoCircle
import com.example.gymbuddy.ui.ScreenCard
import com.example.gymbuddy.ui.StatCard
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(
    viewModel: GymViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val auth = Firebase.auth
    val db = remember { FirebaseFirestore.getInstance() }
    val storage = remember { FirebaseStorage.getInstance() }
    val user = auth.currentUser
    val logs by viewModel.trainingLogs.observeAsState(emptyList())
    val attendedLogs = logs.filter { it.attended }
    val maxStreak = attendedLogs.maxOfOrNull { it.streakCount } ?: 0
    val scope = rememberCoroutineScope()

    var userName by remember { mutableStateOf(user?.displayName ?: "Utilizador (x)") }
    var userEmail by remember { mutableStateOf(user?.email ?: "Utilizador") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var isMale by remember { mutableStateOf(true) }
    var goal by remember { mutableStateOf("maintain") }
    var showNameDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var editedName by remember(userName) { mutableStateOf(userName) }

    LaunchedEffect(user?.uid) {
        val currentUser = auth.currentUser ?: return@LaunchedEffect
        userName = currentUser.displayName ?: "Utilizador (x)"
        userEmail = currentUser.email ?: "Utilizador"

        val prefs = context.getSharedPreferences("gymbuddy_prefs", Context.MODE_PRIVATE)
        age = prefs.getString("user_age", "") ?: ""
        height = prefs.getString("user_height", "") ?: ""
        isMale = prefs.getBoolean("user_is_male", true)
        goal = prefs.getString("user_goal", "maintain") ?: "maintain"

        runCatching { db.collection("users").document(currentUser.uid).get().await() }
            .onSuccess { document ->
                if (document.exists()) {
                    age = document.getString("age") ?: age
                    height = document.getString("height") ?: height
                    isMale = document.getBoolean("isMale") ?: isMale
                    goal = document.getString("goal") ?: goal
                    profileImageUrl = document.getString("profileImageUrl")
                    saveProfileData(context, db, currentUser.uid, currentUser.email.orEmpty(), age, height, isMale, goal)
                }
            }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        val currentUser = auth.currentUser ?: return@rememberLauncherForActivityResult
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val ref = storage.reference.child("profile_images/${currentUser.uid}.jpg")
                ref.putFile(uri).await()
                ref.downloadUrl.await().toString().also { url ->
                    db.collection("users").document(currentUser.uid).update("profileImageUrl", url).await()
                }
            }.onSuccess { url ->
                profileImageUrl = url
            }.onFailure {
                Toast.makeText(context, "Erro ao carregar imagem", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Alterar Nome") },
            text = { AppTextField(value = editedName, onValueChange = { editedName = it }, label = "Nome") },
            confirmButton = {
                TextButton(onClick = {
                    showNameDialog = false
                    val newName = editedName.trim()
                    if (newName.isNotBlank()) {
                        auth.currentUser?.updateProfile(userProfileChangeRequest { displayName = newName })?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                userName = newName
                                Toast.makeText(context, "Nome atualizado!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Limpar Dados") },
            text = { Text("Tens a certeza? Todos os teus planos, treinos e perfil serão apagados permanentemente da nuvem e do telemóvel.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearUserData()
                    context.getSharedPreferences("gymbuddy_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                    age = ""
                    height = ""
                    isMale = true
                    goal = "maintain"
                    profileImageUrl = null
                    Toast.makeText(context, "Todos os dados foram apagados!", Toast.LENGTH_SHORT).show()
                }) { Text("Sim, Limpar Tudo") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancelar") } }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PhotoCircle(
            imageUrl = profileImageUrl,
            placeholder = R.drawable.ic_launcher_foreground,
            onClick = { pickImageLauncher.launch("image/*") }
        )
        TextButton(onClick = { pickImageLauncher.launch("image/*") }) {
            Text("definir imagem de perfil")
        }
        TextButton(onClick = { editedName = userName; showNameDialog = true }) {
            Text(userName, style = MaterialTheme.typography.headlineSmall, color = Color.Black)
        }
        Text(userEmail)

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(
                value = attendedLogs.size.toString(),
                label = "Treinos Feitos",
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                value = maxStreak.toString(),
                label = "Recorde Streak",
                valueColor = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
        }

        Text("CONFIGURAÇÃO CORPORAL", style = MaterialTheme.typography.titleMedium)
        ScreenCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppTextField(
                    value = age,
                    onValueChange = {
                        age = it
                        auth.currentUser?.uid?.let { uid ->
                            saveProfileData(context, db, uid, auth.currentUser?.email.orEmpty(), age, height, isMale, goal)
                        }
                    },
                    label = "Idade"
                )
                AppTextField(
                    value = height,
                    onValueChange = {
                        height = it
                        auth.currentUser?.uid?.let { uid ->
                            saveProfileData(context, db, uid, auth.currentUser?.email.orEmpty(), age, height, isMale, goal)
                        }
                    },
                    label = "Altura (cm)"
                )
                Text("Sexo Biológico:")
                GenderOption(text = "Masculino", selected = isMale) {
                    isMale = true
                    auth.currentUser?.uid?.let { uid ->
                        saveProfileData(context, db, uid, auth.currentUser?.email.orEmpty(), age, height, isMale, goal)
                    }
                }
                GenderOption(text = "Feminino", selected = !isMale) {
                    isMale = false
                    auth.currentUser?.uid?.let { uid ->
                        saveProfileData(context, db, uid, auth.currentUser?.email.orEmpty(), age, height, isMale, goal)
                    }
                }
                Text("Objetivo:")
                GoalOption(text = "Perder Peso", selected = goal == "lose") {
                    goal = "lose"
                    auth.currentUser?.uid?.let { uid ->
                        saveProfileData(context, db, uid, auth.currentUser?.email.orEmpty(), age, height, isMale, goal)
                    }
                }
                GoalOption(text = "Manter Peso", selected = goal == "maintain") {
                    goal = "maintain"
                    auth.currentUser?.uid?.let { uid ->
                        saveProfileData(context, db, uid, auth.currentUser?.email.orEmpty(), age, height, isMale, goal)
                    }
                }
                GoalOption(text = "Ganhar Peso (Massa)", selected = goal == "gain") {
                    goal = "gain"
                    auth.currentUser?.uid?.let { uid ->
                        saveProfileData(context, db, uid, auth.currentUser?.email.orEmpty(), age, height, isMale, goal)
                    }
                }
            }
        }

        OutlinedButton(onClick = {
            val email = auth.currentUser?.email ?: return@OutlinedButton
            auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Email de redefinição enviado para $email", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Erro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Alterar Password")
        }
        TextButton(onClick = {
            NotificationHelper(context).sendNotification(
                999,
                "Teste de Notificação",
                "Se estás a ler isto, as notificações estão a funcionar!"
            )
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Testar Notificações")
        }
        TextButton(onClick = { showClearDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Limpar Dados da Conta", color = Color(0xFFF44336))
        }
        Button(onClick = {
            auth.signOut()
            onLogout()
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Terminar Sessão")
        }
        Spacer(modifier = Modifier.height(88.dp))
    }
}

private fun saveProfileData(
    context: Context,
    db: FirebaseFirestore,
    userId: String,
    email: String,
    age: String,
    height: String,
    isMale: Boolean,
    goal: String
) {
    db.collection("users").document(userId).set(
        hashMapOf(
            "email" to email,
            "age" to age,
            "height" to height,
            "isMale" to isMale,
            "goal" to goal
        )
    )
    context.getSharedPreferences("gymbuddy_prefs", Context.MODE_PRIVATE).edit().apply {
        putString("user_age", age)
        putString("user_height", height)
        putBoolean("user_is_male", isMale)
        putString("user_goal", goal)
        apply()
    }
}

@Composable
private fun GenderOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text)
    }
}

@Composable
private fun GoalOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text)
    }
}
