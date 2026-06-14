package com.example.gymbuddy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gymbuddy.databinding.ActivityAuthBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        updateUI()

        binding.buttonToggleAuth.setOnClickListener {
            isLoginMode = !isLoginMode
            updateUI()
        }

        binding.buttonLogin.setOnClickListener {
            val email = binding.editEmail.text.toString()
            val password = binding.editPassword.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Preencha email e password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading(true)
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    showLoading(false)
                    if (task.isSuccessful) {
                        startMainActivity()
                    } else {
                        Toast.makeText(this, "Erro no Login: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        binding.buttonRegister.setOnClickListener {
            val name = binding.editName.text.toString()
            val email = binding.editEmail.text.toString()
            val password = binding.editPassword.text.toString()

            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showLoading(true)
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val profileUpdates = userProfileChangeRequest {
                            displayName = name
                        }
                        user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                            showLoading(false)
                            Toast.makeText(this, "Registo efetuado! Faça login agora.", Toast.LENGTH_LONG).show()
                            isLoginMode = true
                            updateUI()
                            binding.editPassword.text?.clear()
                            binding.editName.text?.clear()
                        }
                    } else {
                        showLoading(false)
                        Toast.makeText(this, "Erro no Registo: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun updateUI() {
        if (isLoginMode) {
            binding.layoutName.visibility = View.GONE
            binding.buttonLogin.visibility = View.VISIBLE
            binding.buttonRegister.visibility = View.GONE
            binding.buttonToggleAuth.text = "Não tens conta? Regista-te"
        } else {
            binding.layoutName.visibility = View.VISIBLE
            binding.buttonLogin.visibility = View.GONE
            binding.buttonRegister.visibility = View.VISIBLE
            binding.buttonToggleAuth.text = "Já tens conta? Faz Login"
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !isLoading
        binding.buttonRegister.isEnabled = !isLoading
        binding.buttonToggleAuth.isEnabled = !isLoading
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
