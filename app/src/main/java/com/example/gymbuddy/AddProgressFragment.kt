package com.example.gymbuddy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymbuddy.data.ProgressEntry
import com.example.gymbuddy.databinding.FragmentAddProgressBinding

class AddProgressFragment : Fragment() {

    private var _binding: FragmentAddProgressBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GymViewModel by activityViewModels()
    private val selectedPhotoUris = mutableListOf<Uri>()
    private lateinit var photoAdapter: PhotoPreviewAdapter

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        uris.forEach { uri ->
            if (!selectedPhotoUris.contains(uri)) {
                selectedPhotoUris.add(uri)
                // Persist permission
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {}
            }
        }
        photoAdapter.setUris(selectedPhotoUris)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoAdapter = PhotoPreviewAdapter { uri ->
            selectedPhotoUris.remove(uri)
            photoAdapter.setUris(selectedPhotoUris)
        }
        
        binding.recyclerPhotoPreviews.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = photoAdapter
        }

        binding.buttonAddPhotos.setOnClickListener {
            pickImagesLauncher.launch("image/*")
        }

        binding.buttonSaveProgress.setOnClickListener {
            val weight = binding.editWeight.text.toString().toFloatOrNull()
            val bodyFat = binding.editBodyFat.text.toString().toFloatOrNull()
            val notes = binding.editNotes.text.toString()

            if (weight == null) {
                Toast.makeText(requireContext(), "Por favor insira um peso válido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val entry = ProgressEntry(
                date = System.currentTimeMillis(),
                weightKg = weight,
                bodyFatPercentage = bodyFat,
                photoUri = selectedPhotoUris.joinToString(","),
                notes = if (notes.isNotBlank()) notes else null
            )

            viewModel.insertProgressEntry(entry)
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
