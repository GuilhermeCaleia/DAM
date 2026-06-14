package com.example.gymbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.gymbuddy.data.WorkoutPlan
import com.example.gymbuddy.databinding.FragmentSecondBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat

class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GymViewModel by activityViewModels()
    private var selectedHour: String = "08:00"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSelectTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(8)
                .setMinute(0)
                .setTitleText("Selecione a Hora")
                .build()

            picker.addOnPositiveButtonClickListener {
                selectedHour = String.format("%02d:%02d", picker.hour, picker.minute)
                binding.buttonSelectTime.text = "Hora: $selectedHour"
            }

            picker.show(childFragmentManager, "time_picker")
        }

        binding.buttonSave.setOnClickListener {
            val name = binding.editPlanName.text.toString()
            val selectedDays = getSelectedChipTexts(binding.chipGroupDays)
            val selectedMuscles = getSelectedChipTexts(binding.chipGroupMuscles)
            val notes = binding.editNotes.text.toString()
            val isAdditional = binding.switchIsAdditional.isChecked

            if (name.isBlank() || selectedDays.isEmpty() || selectedMuscles.isEmpty()) {
                Toast.makeText(requireContext(), "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val plan = WorkoutPlan(
                name = name,
                days = selectedDays.joinToString(", "),
                muscleGroups = selectedMuscles.joinToString(", "),
                hour = selectedHour,
                isAdditional = isAdditional,
                notes = if (notes.isNotBlank()) notes else null
            )

            viewModel.insertWorkoutPlan(plan)
            findNavController().navigateUp()
        }
    }

    private fun getSelectedChipTexts(chipGroup: ChipGroup): List<String> {
        val selectedTexts = mutableListOf<String>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                selectedTexts.add(chip.text.toString())
            }
        }
        return selectedTexts
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
