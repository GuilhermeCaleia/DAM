package com.example.gymbuddy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import coil.load
import coil.transform.CircleCropTransformation
import com.example.gymbuddy.databinding.FragmentProfileBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GymViewModel by activityViewModels()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploadProfileImage(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val user = Firebase.auth.currentUser
        binding.textUserEmail.text = user?.email ?: "Utilizador"
        
        val displayName = user?.displayName ?: "Utilizador (x)"
        binding.textUserName.text = displayName

        loadProfileImage()
        loadProfileData()

        viewModel.trainingLogs.observe(viewLifecycleOwner) { logs ->
            val attendedLogs = logs.filter { it.attended }
            binding.textTotalWorkouts.text = attendedLogs.size.toString()
            
            val maxStreak = attendedLogs.maxOfOrNull { it.streakCount } ?: 0
            binding.textMaxStreak.text = maxStreak.toString()
        }

        setupListeners()

        binding.textUserName.setOnClickListener {
            showChangeNameDialog()
        }

        binding.textSetImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.imageProfile.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.buttonChangePassword.setOnClickListener {
            val email = user?.email
            if (email != null) {
                Firebase.auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Email de redefinição enviado para $email", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(requireContext(), "Erro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        binding.buttonTestNotifications.setOnClickListener {
            NotificationHelper(requireContext()).sendNotification(
                999, 
                "Teste de Notificação", 
                "Se estás a ler isto, as notificações estão a funcionar!"
            )
        }

        binding.buttonClearData.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Limpar Dados")
                .setMessage("Tens a certeza? Todos os teus planos, treinos e perfil serão apagados permanentemente da nuvem e do telemóvel.")
                .setPositiveButton("Sim, Limpar Tudo") { _, _ ->
                    // 1. Limpar na Nuvem e Base de Dados local (via ViewModel)
                    viewModel.clearUserData()
                    
                    // 2. Limpar Cache Local (SharedPreferences)
                    val prefs = requireContext().getSharedPreferences("gymbuddy_prefs", Context.MODE_PRIVATE)
                    prefs.edit().clear().apply()

                    // 3. Reset da UI
                    binding.editAge.setText("")
                    binding.editHeight.setText("")
                    binding.radioMale.isChecked = true
                    binding.radioMaintain.isChecked = true
                    binding.imageProfile.setImageResource(R.drawable.ic_launcher_foreground)
                    binding.textTotalWorkouts.text = "0"
                    binding.textMaxStreak.text = "0"

                    Toast.makeText(requireContext(), "Todos os dados foram apagados!", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        binding.buttonLogout.setOnClickListener {
            Firebase.auth.signOut()
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun setupListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                saveProfileData()
            }
        }
        binding.editAge.addTextChangedListener(watcher)
        binding.editHeight.addTextChangedListener(watcher)
        binding.radioGroupSex.setOnCheckedChangeListener { _, _ -> saveProfileData() }
        binding.radioGroupGoal.setOnCheckedChangeListener { _, _ -> saveProfileData() }
    }

    private fun saveProfileData() {
        val user = Firebase.auth.currentUser ?: return
        
        val goal = when (binding.radioGroupGoal.checkedRadioButtonId) {
            R.id.radio_lose -> "lose"
            R.id.radio_gain -> "gain"
            else -> "maintain"
        }

        val profileData = hashMapOf(
            "email" to (user.email ?: ""),
            "age" to binding.editAge.text.toString(),
            "height" to binding.editHeight.text.toString(),
            "isMale" to binding.radioMale.isChecked,
            "goal" to goal
        )

        db.collection("users").document(user.uid)
            .set(profileData)
            .addOnFailureListener { e ->
                // Opcional: tratar erro de rede
            }
            
        // Mantemos SharedPreferences como cache rápido local
        val prefs = requireContext().getSharedPreferences("gymbuddy_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("user_age", binding.editAge.text.toString())
            putString("user_height", binding.editHeight.text.toString())
            putBoolean("user_is_male", binding.radioMale.isChecked)
            putString("user_goal", goal)
            apply()
        }
    }

    private fun loadProfileData() {
        val user = Firebase.auth.currentUser ?: return
        
        // 1. Carregar rápido do cache local
        val prefs = requireContext().getSharedPreferences("gymbuddy_prefs", Context.MODE_PRIVATE)
        binding.editAge.setText(prefs.getString("user_age", ""))
        binding.editHeight.setText(prefs.getString("user_height", ""))
        val isMale = prefs.getBoolean("user_is_male", true)
        if (isMale) binding.radioMale.isChecked = true else binding.radioFemale.isChecked = true
        
        val goal = prefs.getString("user_goal", "maintain")
        updateGoalRadio(goal)

        // 2. Carregar do Firestore (Nuvem) para sincronizar entre dispositivos
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val age = document.getString("age") ?: ""
                    val height = document.getString("height") ?: ""
                    val isMaleCloud = document.getBoolean("isMale") ?: true
                    val goalCloud = document.getString("goal") ?: "maintain"

                    binding.editAge.setText(age)
                    binding.editHeight.setText(height)
                    if (isMaleCloud) binding.radioMale.isChecked = true else binding.radioFemale.isChecked = true
                    updateGoalRadio(goalCloud)
                    
                    // Atualizar cache local
                    prefs.edit().apply {
                        putString("user_age", age)
                        putString("user_height", height)
                        putBoolean("user_is_male", isMaleCloud)
                        putString("user_goal", goalCloud)
                        apply()
                    }
                }
            }
    }

    private fun updateGoalRadio(goal: String?) {
        when (goal) {
            "lose" -> binding.radioLose.isChecked = true
            "gain" -> binding.radioGain.isChecked = true
            else -> binding.radioMaintain.isChecked = true
        }
    }

    private fun showChangeNameDialog() {
        val editText = EditText(requireContext())
        editText.setText(binding.textUserName.text)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Alterar Nome")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->
                val newName = editText.text.toString()
                if (newName.isNotBlank()) {
                    updateUserName(newName)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateUserName(newName: String) {
        val user = Firebase.auth.currentUser
        val profileUpdates = userProfileChangeRequest {
            displayName = newName
        }
        user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                binding.textUserName.text = newName
                Toast.makeText(requireContext(), "Nome atualizado!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadProfileImage(uri: Uri) {
        val user = Firebase.auth.currentUser ?: return
        val ref = storage.reference.child("profile_images/${user.uid}.jpg")

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri: Uri ->
                    saveImageUrlToFirestore(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Erro ao carregar imagem", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveImageUrlToFirestore(url: String) {
        val user = Firebase.auth.currentUser ?: return
        db.collection("users").document(user.uid)
            .update("profileImageUrl", url)
            .addOnSuccessListener {
                binding.imageProfile.load(url) {
                    transformations(CircleCropTransformation())
                }
            }
    }

    private fun loadProfileImage() {
        val user = Firebase.auth.currentUser ?: return
        
        // Carregar do Firestore
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { document ->
                val url = document.getString("profileImageUrl")
                if (!url.isNullOrEmpty()) {
                    binding.imageProfile.load(url) {
                        transformations(CircleCropTransformation())
                        placeholder(R.drawable.ic_launcher_foreground)
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
