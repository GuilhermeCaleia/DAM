package com.example.gymbuddy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.gymbuddy.data.TrainingLog
import com.example.gymbuddy.data.WorkoutPlan
import com.example.gymbuddy.databinding.ItemWorkoutPlanBinding
import java.util.Calendar

class WorkoutAdapter(
    private val onMarkDone: (WorkoutPlan) -> Unit,
    private val onDeletePlan: (WorkoutPlan) -> Unit
) : ListAdapter<WorkoutPlan, WorkoutAdapter.WorkoutViewHolder>(WorkoutDiffCallback()) {

    private var trainingLogs: List<TrainingLog> = emptyList()

    fun updateLogs(newLogs: List<TrainingLog>) {
        trainingLogs = newLogs
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        return WorkoutViewHolder(
            ItemWorkoutPlanBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onMarkDone,
            onDeletePlan
        )
    }

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        holder.bind(getItem(position), trainingLogs)
    }

    class WorkoutViewHolder(
        private val binding: ItemWorkoutPlanBinding,
        private val onMarkDone: (WorkoutPlan) -> Unit,
        private val onDeletePlan: (WorkoutPlan) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(plan: WorkoutPlan, logs: List<TrainingLog>) {
            binding.textPlanName.text = plan.name
            
            val displayDays = plan.days.split(", ").joinToString(", ") { day ->
                when (day) {
                    "Monday" -> "Seg"
                    "Tuesday" -> "Ter"
                    "Wednesday" -> "Qua"
                    "Thursday" -> "Qui"
                    "Friday" -> "Sex"
                    "Saturday" -> "Sáb"
                    "Sunday" -> "Dom"
                    else -> day
                }
            }
            binding.textDays.text = "$displayDays às ${plan.hour}"
            binding.buttonMarkDone.setOnClickListener { onMarkDone(plan) }
            
            binding.root.setOnLongClickListener {
                AlertDialog.Builder(binding.root.context)
                    .setTitle("Eliminar Plano")
                    .setMessage("Queres eliminar o plano '${plan.name}'?")
                    .setPositiveButton("Sim") { _, _ -> onDeletePlan(plan) }
                    .setNegativeButton("Não", null)
                    .show()
                true
            }
            
            // Check if done this week
            val isDoneThisWeek = logs.any { log -> 
                log.workoutPlanId == plan.id && isWithinCurrentWeek(log.date)
            }

            if (isDoneThisWeek) {
                binding.statusCircle.setBackgroundResource(R.drawable.circle_done)
                binding.buttonMarkDone.isEnabled = false
                binding.buttonMarkDone.text = "Feito"
                binding.buttonMarkDone.alpha = 0.5f
            } else {
                binding.statusCircle.setBackgroundResource(R.drawable.circle_missed)
                binding.buttonMarkDone.isEnabled = true
                binding.buttonMarkDone.text = "Fiz"
                binding.buttonMarkDone.alpha = 1.0f
            }

            // Highlight additional workouts
            if (plan.isAdditional) {
                binding.root.setBackgroundColor(0x11FFEB3B.toInt()) // Very light yellow
            } else {
                binding.root.setBackgroundColor(0)
            }
        }

        private fun isWithinCurrentWeek(time: Long): Boolean {
            val cal = Calendar.getInstance()
            cal.firstDayOfWeek = Calendar.MONDAY
            while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            
            val calEnd = cal.clone() as Calendar
            calEnd.add(Calendar.DAY_OF_YEAR, 7)
            val end = calEnd.timeInMillis
            
            return time in start until end
        }
    }

    class WorkoutDiffCallback : DiffUtil.ItemCallback<WorkoutPlan>() {
        override fun areItemsTheSame(oldItem: WorkoutPlan, newItem: WorkoutPlan): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WorkoutPlan, newItem: WorkoutPlan): Boolean {
            return oldItem == newItem
        }
    }
}
