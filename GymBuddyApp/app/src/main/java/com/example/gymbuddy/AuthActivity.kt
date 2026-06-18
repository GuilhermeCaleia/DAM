package com.example.gymbuddy

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.gymbuddy.ui.AppBackground
import com.example.gymbuddy.ui.AppTextField
import com.example.gymbuddy.ui.GymBuddyTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest

class AuthActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        setContent {
            GymBuddyTheme {
                AuthScreen(
                    onLogin = { email, password, setLoading ->
                        showLoadingLogin(email, password, setLoading)
                    },
                    onRegister = { name, email, password, clearFields, setLoading, switchToLogin ->
                        registerUser(name, email, password, clearFields, setLoading, switchToLogin)
                    }
                )
            }
        }
    }

    private fun showLoadingLogin(
        email: String,
        password: String,
        setLoading: (Boolean) -> Unit
    ) {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Preencha email e password", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    startMainActivity()
                } else {
                    Toast.makeText(this, "Erro no Login: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun registerUser(
        name: String,
        email: String,
        password: String,
        clearFields: () -> Unit,
        setLoading: (Boolean) -> Unit,
        switchToLogin: () -> Unit
    ) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = userProfileChangeRequest {
                        displayName = name
                    }
                    user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                        setLoading(false)
                        Toast.makeText(this, "Registo efetuado! Faça login agora.", Toast.LENGTH_LONG).show()
                        clearFields()
                        switchToLogin()
                    }
                } else {
                    setLoading(false)
                    Toast.makeText(this, "Erro no Registo: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@Composable
private fun AuthScreen(
    onLogin: (String, String, (Boolean) -> Unit) -> Unit,
    onRegister: (String, String, String, () -> Unit, (Boolean) -> Unit, () -> Unit) -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            Text(
                text = stringResource(id = R.string.auth_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(48.dp))
            if (!isLoginMode) {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(id = R.string.auth_name),
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            AppTextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(id = R.string.auth_email),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(id = R.string.auth_password)) },
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = {
                    if (isLoginMode) {
                        onLogin(email, password) { isLoading = it }
                    } else {
                        onRegister(
                            name,
                            email,
                            password,
                            {
                                password = ""
                                name = ""
                            },
                            { isLoading = it },
                            { isLoginMode = true }
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(stringResource(id = if (isLoginMode) R.string.auth_login else R.string.auth_register))
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = { isLoginMode = !isLoginMode },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        id = if (isLoginMode) R.string.auth_switch_to_register else R.string.auth_switch_to_login
                    )
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Image(
                painter = painterResource(id = R.drawable.gymbuddy),
                contentDescription = stringResource(id = R.string.auth_mascot),
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
