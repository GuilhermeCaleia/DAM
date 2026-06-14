package com.example.gymbuddy

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.gymbuddy.data.ProgressEntry
import com.example.gymbuddy.databinding.FragmentHistoryBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GymViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupChart()

        val adapter = ProgressAdapter { entry ->
            showDetailDialog(entry)
        }
        binding.recyclerviewHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerviewHistory.adapter = adapter

        viewModel.progressEntries.observe(viewLifecycleOwner) { entries ->
            adapter.submitList(entries)
            updateCalorieCalculation(entries.firstOrNull()?.weightKg)
            updateChart(entries)
        }
    }

    private fun setupChart() {
        binding.weightChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            setExtraOffsets(10f, 10f, 10f, 20f)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.GRAY
                granularity = 1f // Garantir que mostra 1 ponto de cada vez
                labelRotationAngle = -45f // Inclinar as datas para caberem melhor
            }

            axisLeft.apply {
                textColor = Color.GRAY
                setDrawGridLines(true)
                gridColor = Color.LTGRAY
                // Dar um pequeno espaço acima e abaixo do peso
                spaceTop = 20f
                spaceBottom = 20f
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
        }
    }

    private fun updateChart(entries: List<ProgressEntry>) {
        if (entries.isEmpty()) {
            binding.weightChart.clear()
            return
        }

        // Ordenar do mais antigo para o mais recente para o gráfico
        val sortedEntries = entries.sortedBy { it.date }
        
        // Criar pontos usando o INDEX (0, 1, 2...) para evitar erro de escala
        val chartEntries = sortedEntries.mapIndexed { index, entry ->
            Entry(index.toFloat(), entry.weightKg)
        }

        // Formatar as datas no eixo X com base no index
        binding.weightChart.xAxis.valueFormatter = object : ValueFormatter() {
            private val mFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < sortedEntries.size) {
                    mFormat.format(Date(sortedEntries[index].date))
                } else ""
            }
        }

        val dataSet = LineDataSet(chartEntries, "Peso").apply {
            color = ContextCompat.getColor(requireContext(), R.color.gym_green)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.gym_green))
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(true)
            valueTextSize = 10f
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.chart_fill_gradient)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(true) // Mostrar o peso por cima do ponto
        }

        binding.weightChart.data = LineData(dataSet)
        
        // Ajustar a vista para o último ponto
        binding.weightChart.setVisibleXRangeMaximum(5f) // Mostrar no máximo 5 pontos de uma vez
        binding.weightChart.moveViewToX(chartEntries.size.toFloat())

        binding.weightChart.invalidate()
    }

    private fun showDetailDialog(entry: ProgressEntry) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_progress_detail, null)
        val recyclerImages = dialogView.findViewById<RecyclerView>(R.id.recycler_detail_images)
        val textDate = dialogView.findViewById<TextView>(R.id.text_detail_date)
        val textStats = dialogView.findViewById<TextView>(R.id.text_detail_stats)
        val textNotes = dialogView.findViewById<TextView>(R.id.text_detail_notes)

        val dateFormat = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "PT"))
        textDate.text = dateFormat.format(Date(entry.date))
        
        val stats = StringBuilder("${entry.weightKg} kg")
        if (entry.bodyFatPercentage != null) {
            stats.append("  |  ${entry.bodyFatPercentage}% IMG")
        }
        textStats.text = stats.toString()
        textNotes.text = entry.notes ?: "Sem observações."
        
        val imageUrls = entry.photoUri?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        if (imageUrls.isNotEmpty()) {
            recyclerImages.visibility = View.VISIBLE
            recyclerImages.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            recyclerImages.adapter = DetailImageAdapter(imageUrls)
        } else {
            recyclerImages.visibility = View.GONE
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Fechar", null)
            .setNegativeButton("Eliminar") { _, _ ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Confirmar")
                    .setMessage("Queres mesmo eliminar este registo?")
                    .setPositiveButton("Sim") { _, _ ->
                        viewModel.deleteProgressEntry(entry)
                    }
                    .setNegativeButton("Não", null)
                    .show()
            }
            .show()
    }

    private fun updateCalorieCalculation(weight: Float?) {
        val prefs = requireContext().getSharedPreferences("gymbuddy_prefs", Context.MODE_PRIVATE)
        val age = prefs.getString("user_age", "")?.toIntOrNull()
        val height = prefs.getString("user_height", "")?.toIntOrNull()
        val isMale = prefs.getBoolean("user_is_male", true)
        val goal = prefs.getString("user_goal", "maintain")

        if (weight != null && age != null && height != null) {
            binding.textSetupHint.visibility = View.GONE
            
            val bmr = if (isMale) {
                10 * weight + 6.25 * height - 5 * age + 5
            } else {
                10 * weight + 6.25 * height - 5 * age - 161
            }

            val tdee = bmr * 1.375
            val targetCalories = when (goal) {
                "lose" -> tdee - 500
                "gain" -> tdee + 300
                else -> tdee
            }

            binding.textCaloriesResult.text = "${targetCalories.toInt()} KCAL"
            binding.textGoalLabel.text = when (goal) {
                "lose" -> "RECOMENDAÇÃO: PERDER PESO"
                "gain" -> "RECOMENDAÇÃO: GANHAR PESO"
                else -> "RECOMENDAÇÃO: MANTER PESO"
            }
        } else {
            binding.textCaloriesResult.text = "--- KCAL"
            binding.textSetupHint.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
