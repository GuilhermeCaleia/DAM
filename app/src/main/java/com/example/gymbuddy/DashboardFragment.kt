package com.example.gymbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.gymbuddy.data.WorkoutPlan
import com.example.gymbuddy.databinding.FragmentDashboardBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GymViewModel by viewModels()
    
    private var nextPlan: WorkoutPlan? = null
    private var nextPlanDate: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.trainingLogs.observe(viewLifecycleOwner) { logs ->
            val streak = logs.firstOrNull()?.streakCount ?: 0
            binding.textStreakCount.text = "$streak dias seguidos!"
            updateWeekDots()
            updateNextWorkout(viewModel.workoutPlans.value ?: emptyList())
        }
        
        viewModel.workoutPlans.observe(viewLifecycleOwner) { plans ->
            updateNextWorkout(plans)
            updateWeekDots()
        }

        // Removed click listener to mark workout as done from dashboard.
        // Workouts can now only be marked as done in the Workout Plans screen.

        // Get user's first name
        val fullName = Firebase.auth.currentUser?.displayName ?: "Utilizador"
        val firstName = fullName.split(" ").firstOrNull() ?: fullName
        binding.textWelcome.text = "Olá $firstName"
    }

    private fun updateWeekDots() {
        val plans = viewModel.workoutPlans.value ?: return
        val logs = viewModel.trainingLogs.value ?: emptyList()

        val dots = listOf(
            binding.dotDay1, binding.dotDay2, binding.dotDay3,
            binding.dotDay4, binding.dotDay5, binding.dotDay6, binding.dotDay7
        )

        // Find Monday of the current week
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        // Update week range text
        val startDay = cal.get(Calendar.DAY_OF_MONTH)
        val startMonth = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "PT"))
        
        val calEnd = cal.clone() as Calendar
        calEnd.add(Calendar.DAY_OF_YEAR, 6)
        val endDay = calEnd.get(Calendar.DAY_OF_MONTH)
        val endMonth = calEnd.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("pt", "PT"))
        
        binding.textWeekRange.text = if (startMonth == endMonth) {
            "$startDay-$endDay $startMonth"
        } else {
            "$startDay $startMonth - $endDay $endMonth"
        }

        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        for (i in 0 until 7) {
            val dayStartTime = cal.timeInMillis
            val nextDayCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            val dayEndTime = nextDayCal.timeInMillis
            
            val dayName = dayNames[i]

            // Logs in this 24h window
            val dayLogs = logs.filter { it.date in dayStartTime until dayEndTime }
            
            val hasRegularLog = dayLogs.any { !it.isAdditional }
            val hasAdditionalLog = dayLogs.any { it.isAdditional }
            
            // Is any regular plan scheduled for this day name?
            val isScheduled = plans.any { !it.isAdditional && it.days.contains(dayName) }

            val background = when {
                hasRegularLog || hasAdditionalLog -> R.drawable.circle_done // Green if any workout done
                isScheduled -> R.drawable.circle_missed // Red if scheduled but not done
                else -> R.drawable.circle_pending // Grey if nothing scheduled and nothing done
            }

            dots[i].setBackgroundResource(background)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    private fun updateNextWorkout(plans: List<WorkoutPlan>) {
        val logs = viewModel.trainingLogs.value ?: emptyList()
        if (plans.isEmpty()) {
            binding.textNextWorkoutName.text = "Nenhum plano definido"
            binding.textNextWorkoutTime.text = "-"
            nextPlan = null
            nextPlanDate = null
            return
        }

        val now = Calendar.getInstance()
        val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        
        val dayMap = mapOf(
            "Monday" to Calendar.MONDAY,
            "Tuesday" to Calendar.TUESDAY,
            "Wednesday" to Calendar.WEDNESDAY,
            "Thursday" to Calendar.THURSDAY,
            "Friday" to Calendar.FRIDAY,
            "Saturday" to Calendar.SATURDAY,
            "Sunday" to Calendar.SUNDAY
        )

        var bestPlan: WorkoutPlan? = null
        var minDiff = Int.MAX_VALUE
        var bestDate: Calendar? = null

        for (plan in plans) {
            val planDays = plan.days.split(", ")
            for (dayStr in planDays) {
                val dayInt = dayMap[dayStr] ?: continue
                
                var diff = dayInt - currentDayOfWeek
                if (diff < 0) diff += 7
                
                if (diff == 0) {
                    val timeParts = plan.hour.split(":")
                    val planTimeToday = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                        set(Calendar.MINUTE, timeParts[1].toInt())
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    if (now.after(planTimeToday)) {
                        diff = 7 
                    }
                }

                // Check if this specific occurrence is already done
                val checkDate = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, diff)
                    val timeParts = plan.hour.split(":")
                    set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    set(Calendar.MINUTE, timeParts[1].toInt())
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val isAlreadyDone = logs.any { it.workoutPlanId == plan.id && isSameDay(it.date, checkDate.timeInMillis) }
                
                // If done today, look for the next occurrence (in 7 days)
                var finalDiff = diff
                var finalCheckDate = checkDate
                if (isAlreadyDone) {
                    finalDiff += 7
                    finalCheckDate = (checkDate.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 7) }
                }

                if (finalDiff < minDiff) {
                    minDiff = finalDiff
                    bestPlan = plan
                    bestDate = finalCheckDate
                }
            }
        }

        if (bestPlan != null && bestDate != null) {
            binding.textNextWorkoutName.text = bestPlan.muscleGroups
            val sdf = SimpleDateFormat("EEE, dd MMM • HH:mm", Locale.getDefault())
            binding.textNextWorkoutTime.text = sdf.format(bestDate.time).replaceFirstChar { it.uppercase() }
            nextPlan = bestPlan
            nextPlanDate = bestDate.timeInMillis
            binding.cardNextWorkout.alpha = 1.0f
            binding.cardNextWorkout.isEnabled = true
        }
    }

    private fun isSameDay(date1: Long, date2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
