package com.example.gymbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gymbuddy.data.WorkoutPlan
import com.example.gymbuddy.databinding.FragmentFirstBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GymViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.refreshSync()
        updateWeekRange()

        val regularAdapter = WorkoutAdapter(
            onMarkDone = { plan ->
                viewModel.markAttendance(plan, calculateTargetDate(plan))
                Toast.makeText(requireContext(), "Treino concluído!", Toast.LENGTH_SHORT).show()
            },
            onDeletePlan = { plan ->
                viewModel.deleteWorkoutPlan(plan)
                Toast.makeText(requireContext(), "Plano eliminado", Toast.LENGTH_SHORT).show()
            }
        )
        val additionalAdapter = WorkoutAdapter(
            onMarkDone = { plan ->
                viewModel.markAttendance(plan)
                Toast.makeText(requireContext(), "Treino adicional concluído!", Toast.LENGTH_SHORT).show()
            },
            onDeletePlan = { plan ->
                viewModel.deleteWorkoutPlan(plan)
                Toast.makeText(requireContext(), "Plano eliminado", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerviewWorkoutsRegular.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerviewWorkoutsRegular.adapter = regularAdapter

        binding.recyclerviewWorkoutsAdditional.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerviewWorkoutsAdditional.adapter = additionalAdapter

        viewModel.workoutPlans.observe(viewLifecycleOwner) { plans ->
            val regularPlans = plans.filter { !it.isAdditional }
            val additionalPlans = plans.filter { it.isAdditional }

            regularAdapter.submitList(regularPlans)
            additionalAdapter.submitList(additionalPlans)

            binding.textAdditionalTitle.visibility = View.VISIBLE
            binding.recyclerviewWorkoutsAdditional.visibility = if (additionalPlans.isEmpty()) View.GONE else View.VISIBLE

            binding.textviewEmpty.visibility = if (plans.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.trainingLogs.observe(viewLifecycleOwner) { logs ->
            regularAdapter.updateLogs(logs)
            additionalAdapter.updateLogs(logs)
        }
    }

    private fun calculateTargetDate(plan: WorkoutPlan): Long {
        val now = Calendar.getInstance()
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val dayMap = mapOf(
            "Monday" to 0, "Tuesday" to 1, "Wednesday" to 2, "Thursday" to 3,
            "Friday" to 4, "Saturday" to 5, "Sunday" to 6
        )

        val planDays = plan.days.split(", ")
        val currentDayName = SimpleDateFormat("EEEE", Locale.ENGLISH).format(now.time)
        
        val targetDayName = if (planDays.contains(currentDayName)) {
            currentDayName
        } else {
            val daysInPast = planDays.filter { dayName ->
                val targetDayIdx = dayMap[dayName] ?: 0
                val currentDayIdx = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday=0, ..., Sunday=6
                targetDayIdx <= currentDayIdx
            }.sortedByDescending { dayMap[it] }
            
            daysInPast.firstOrNull() ?: planDays.first()
        }

        val offset = dayMap[targetDayName] ?: 0
        cal.add(Calendar.DAY_OF_YEAR, offset)

        val timeParts = plan.hour.split(":")
        if (timeParts.size == 2) {
            cal.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            cal.set(Calendar.MINUTE, timeParts[1].toInt())
        }

        return cal.timeInMillis
    }

    private fun updateWeekRange() {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        val startDay = cal.get(Calendar.DAY_OF_MONTH)
        val startMonth = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "PT"))
        
        val calEnd = cal.clone() as Calendar
        calEnd.add(Calendar.DAY_OF_YEAR, 6)
        val endDay = calEnd.get(Calendar.DAY_OF_MONTH)
        val endMonth = calEnd.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "PT"))
        
        val rangeText = if (startMonth == endMonth) {
            "$startDay-$endDay $startMonth"
        } else {
            "$startDay $startMonth - $endDay $endMonth"
        }
        
        binding.textWeekRange.text = rangeText
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
