package com.example.gymbuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.gymbuddy.data.ProgressEntry
import com.example.gymbuddy.databinding.ItemProgressEntryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProgressAdapter(private val onItemClick: (ProgressEntry) -> Unit) : 
    ListAdapter<ProgressEntry, ProgressAdapter.ProgressViewHolder>(ProgressDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
        return ProgressViewHolder(
            ItemProgressEntryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onItemClick
        )
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProgressViewHolder(
        private val binding: ItemProgressEntryBinding,
        private val onItemClick: (ProgressEntry) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun bind(entry: ProgressEntry) {
            binding.root.setOnClickListener { onItemClick(entry) }
            
            binding.textDate.text = dateFormat.format(Date(entry.date))
            binding.textWeight.text = "${entry.weightKg} kg"
            
            if (entry.bodyFatPercentage != null) {
                binding.textBodyFat.visibility = View.VISIBLE
                binding.textBodyFat.text = "${entry.bodyFatPercentage}% IMG"
            } else {
                binding.textBodyFat.visibility = View.GONE
            }

            binding.textNotes.text = entry.notes ?: ""
            binding.textNotes.visibility = if (entry.notes.isNullOrBlank()) View.GONE else View.VISIBLE

            val firstPhoto = entry.photoUri?.split(",")?.firstOrNull { it.isNotBlank() }
            if (!firstPhoto.isNullOrBlank()) {
                binding.imageProgress.visibility = View.VISIBLE
                binding.imageProgress.load(firstPhoto) {
                    crossfade(true)
                    placeholder(R.color.gym_green)
                    error(android.R.drawable.ic_menu_report_image)
                }
            } else {
                binding.imageProgress.visibility = View.GONE
            }
        }
    }

    class ProgressDiffCallback : DiffUtil.ItemCallback<ProgressEntry>() {
        override fun areItemsTheSame(oldItem: ProgressEntry, newItem: ProgressEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProgressEntry, newItem: ProgressEntry): Boolean {
            return oldItem == newItem
        }
    }
}
