package com.example.gymbuddy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.gymbuddy.ui.AppBackground
import com.example.gymbuddy.ui.AppTextField
import com.example.gymbuddy.ui.GymBuddyTheme
import com.example.gymbuddy.ui.PhotoCircle
import com.example.gymbuddy.ui.ScreenCard
import com.example.gymbuddy.ui.StatCard
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProfileFragment : Fragment() {

    private val viewModel: GymViewModel by activityViewModels()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var userName by mutableStateOf("Utilizador")
    private var userEmail by mutableStateOf("Utilizador")
    private var profileImageUrl by mutableStateOf<String?>(null)
    private var age by mutableStateOf("")
    private var height by mutableStateOf("")
    private var isMale by mutableStateOf(true)
    private var goal by mutableStateOf("maintain")

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadProfileImage(it) }
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        initializeProfileState()
        setContent {
            GymBuddyTheme {
                ProfileScreen()
            }
        }
    }

    private fun initializeProfileState() {
        val user = Firebase.auth.currentUser
        userEmail = user?.email ?: "Utilizador"
        userName = user?.displayName ?: "Utilizador (x)"
        loadProfileData()
        loadProfileImage()
    }

    private fun saveProfileData() {
        val user = Firebase.auth.currentUser ?: return
        val profileData = hashMapOf(
            "email" to (user.email ?: ""),
            "age" to age,
            "height" to height,
            "isMale" to isMale,
            "goal" to goal
        )

        db.collection("users").document(user.uid).set(profileData)
        val prefs = requireContext().getSharedPreferences("gymbuddy_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("user_age", age)
            putString("user_height", height)
            putBoolean("user_is_male", isMale)
            putString("user_goal", goal)
            apply()
        }
    }

    private fun loadProfileData() {
        val user = Firebase.auth.currentUser ?: return
        val prefs = requireContext().getSharedPreferences("gymbuddy_prefs", Context.MODE_PRIVATE)
        age = prefs.getString("user_age", "") ?: ""
        height = prefs.getString("user_height", "") ?: ""
        isMale = prefs.getBoolean("user_is_male", true)
        goal = prefs.getString("user_goal", "maintain") ?: "maintain"

        db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                age = document.getString("age") ?: ""
                height = document.getString("height") ?: ""
                isMale = document.getBoolean("isMale") ?: true
                goal = document.getString("goal") ?: "maintain"
                saveProfileData()
            }
        }
    }

    private fun updateUserName(newName: String) {
        val user = Firebase.auth.currentUser
        val profileUpdates = userProfileChangeRequest { displayName = newName }
        user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                userName = newName
                Toast.makeText(requireContext(), "Nome atualizado!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadProfileImage(uri: Uri) {
        val user = Firebase.auth.currentUser ?: return
        val ref = storage.reference.child("profile_images/${user.uid}.jpg")
        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveImageUrlToFirestore(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao carregar imagem", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveImageUrlToFirestore(url: String) {
        val user = Firebase.auth.currentUser ?: return
        db.collection("users").document(user.uid).update("profileImageUrl", url)
            .addOnSuccessListener { profileImageUrl = url }
    }

    private fun loadProfileImage() {
        val user = Firebase.auth.currentUser ?: return
        db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
            profileImageUrl = document.getString("profileImageUrl")
        }
    }

    private fun clearAllData() {
        viewModel.clearUserData()
        val prefs = requireContext().getSharedPreferences("gymbuddy_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        age = ""
        height = ""
        isMale = true
        goal = "maintain"
        profileImageUrl = null
        Toast.makeText(requireContext(), "Todos os dados foram apagados!", Toast.LENGTH_SHORT).show()
    }

    private fun sendPasswordReset() {
        val email = Firebase.auth.currentUser?.email ?: return
        Firebase.auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(requireContext(), "Email de redefinição enviado para $email", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Erro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logout() {
        Firebase.auth.signOut()
        val intent = Intent(requireContext(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun testNotification() {
        NotificationHelper(requireContext()).sendNotification(
            999,
            "Teste de Notificação",
            "Se estás a ler isto, as notificações estão a funcionar!"
        )
    }

    @Composable
    private fun ProfileScreen() {
        val logs by viewModel.trainingLogs.observeAsState(emptyList())
        var showNameDialog by remember { mutableStateOf(false) }
        var showClearDialog by remember { mutableStateOf(false) }
        var editedName by remember(userName) { mutableStateOf(userName) }
        val attendedLogs = logs.filter { it.attended }
        val maxStreak = attendedLogs.maxOfOrNull { it.streakCount } ?: 0

        if (showNameDialog) {
            AlertDialog(
                onDismissRequest = { showNameDialog = false },
                title = { Text("Alterar Nome") },
                text = { AppTextField(value = editedName, onValueChange = { editedName = it }, label = "Nome") },
                confirmButton = {
                    TextButton(onClick = {
                        showNameDialog = false
                        if (editedName.isNotBlank()) updateUserName(editedName)
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
                confirmButton = { TextButton(onClick = { showClearDialog = false; clearAllData() }) { Text("Sim, Limpar Tudo") } },
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
                    AppTextField(value = age, onValueChange = { age = it; saveProfileData() }, label = "Idade")
                    AppTextField(value = height, onValueChange = { height = it; saveProfileData() }, label = "Altura (cm)")
                    Text("Sexo Biológico:")
                    GenderOption(text = "Masculino", selected = isMale) { isMale = true; saveProfileData() }
                    GenderOption(text = "Feminino", selected = !isMale) { isMale = false; saveProfileData() }
                    Text("Objetivo:")
                    GoalOption(text = "Perder Peso", selected = goal == "lose") { goal = "lose"; saveProfileData() }
                    GoalOption(text = "Manter Peso", selected = goal == "maintain") { goal = "maintain"; saveProfileData() }
                    GoalOption(text = "Ganhar Peso (Massa)", selected = goal == "gain") { goal = "gain"; saveProfileData() }
                }
            }

            OutlinedButton(onClick = { sendPasswordReset() }, modifier = Modifier.fillMaxWidth()) {
                Text("Alterar Password")
            }
            TextButton(onClick = { testNotification() }, modifier = Modifier.fillMaxWidth()) {
                Text("Testar Notificações")
            }
            TextButton(onClick = { showClearDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Limpar Dados da Conta", color = Color(0xFFF44336))
            }
            Button(onClick = { logout() }, modifier = Modifier.fillMaxWidth()) {
                Text("Terminar Sessão")
            }
            Spacer(modifier = Modifier.height(88.dp))
        }
    }

    @Composable
    private fun GenderOption(text: String, selected: Boolean, onClick: () -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Text(text)
        }
    }

    @Composable
    private fun GoalOption(text: String, selected: Boolean, onClick: () -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Text(text)
        }
    }
}
