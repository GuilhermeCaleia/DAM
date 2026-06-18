package com.example.gymbuddy.ui.screens

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.gymbuddy.R
import com.example.gymbuddy.data.ProgressEntry
import com.example.gymbuddy.ui.AppBackground
import com.example.gymbuddy.ui.ScreenCard
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    entries: List<ProgressEntry>,
    onDeleteEntry: (ProgressEntry) -> Unit
) {
    var selectedEntry by remember { mutableStateOf<ProgressEntry?>(null) }
    val weight = entries.firstOrNull()?.weightKg
    val calorieInfo = calorieInfo(LocalContext.current, weight)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("PROGRESSO", style = MaterialTheme.typography.headlineSmall)

        ScreenCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(calorieInfo.result, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text(calorieInfo.label, style = MaterialTheme.typography.labelMedium)
                if (calorieInfo.showHint) {
                    Text("Configure o seu perfil para ver o cálculo", color = Color(0xFFF44336), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Text("EVOLUÇÃO DO PESO (KG)", style = MaterialTheme.typography.titleMedium)
        ScreenCard(modifier = Modifier.fillMaxWidth()) {
            WeightChart(entries = entries, modifier = Modifier.fillMaxWidth().height(250.dp).padding(8.dp))
        }

        Text("HISTÓRICO", style = MaterialTheme.typography.titleMedium)
        if (entries.isEmpty()) {
            Text("Ainda não existem registos de progresso.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                entries.forEach { entry ->
                    ProgressEntryCard(entry = entry, onClick = { selectedEntry = entry })
                }
            }
        }

        Spacer(modifier = Modifier.height(88.dp))
    }

    selectedEntry?.let { entry ->
        ProgressDetailDialog(
            entry = entry,
            onDismiss = { selectedEntry = null },
            onDelete = {
                onDeleteEntry(entry)
                selectedEntry = null
            }
        )
    }
}

private data class CalorieUi(val result: String, val label: String, val showHint: Boolean)

private fun calorieInfo(context: Context, weight: Float?): CalorieUi {
    val prefs = context.getSharedPreferences("gymbuddy_prefs", Context.MODE_PRIVATE)
    val age = prefs.getString("user_age", "")?.toIntOrNull()
    val height = prefs.getString("user_height", "")?.toIntOrNull()
    val isMale = prefs.getBoolean("user_is_male", true)
    val goal = prefs.getString("user_goal", "maintain")

    if (weight != null && age != null && height != null) {
        val bmr = if (isMale) 10 * weight + 6.25 * height - 5 * age + 5 else 10 * weight + 6.25 * height - 5 * age - 161
        val tdee = bmr * 1.375
        val targetCalories = when (goal) {
            "lose" -> tdee - 500
            "gain" -> tdee + 300
            else -> tdee
        }
        val label = when (goal) {
            "lose" -> "RECOMENDAÇÃO: PERDER PESO"
            "gain" -> "RECOMENDAÇÃO: GANHAR PESO"
            else -> "RECOMENDAÇÃO: MANTER PESO"
        }
        return CalorieUi("${targetCalories.toInt()} KCAL", label, false)
    }

    return CalorieUi("--- KCAL", "RECOMENDAÇÃO DIÁRIA", true)
}

@Composable
private fun WeightChart(entries: List<ProgressEntry>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx -> LineChart(ctx).apply { setupChartStyle() } },
        update = { chart -> updateChart(chart, entries, context) },
        modifier = modifier
    )
}

private fun LineChart.setupChartStyle() {
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
        textColor = AndroidColor.GRAY
        granularity = 1f
        labelRotationAngle = -45f
    }

    axisLeft.apply {
        textColor = AndroidColor.GRAY
        setDrawGridLines(true)
        gridColor = AndroidColor.LTGRAY
        spaceTop = 20f
        spaceBottom = 20f
    }

    axisRight.isEnabled = false
    legend.isEnabled = false
}

private fun updateChart(chart: LineChart, entries: List<ProgressEntry>, context: Context) {
    if (entries.isEmpty()) {
        chart.clear()
        return
    }

    val sortedEntries = entries.sortedBy { it.date }
    val chartEntries = sortedEntries.mapIndexed { index, entry -> Entry(index.toFloat(), entry.weightKg) }

    chart.xAxis.valueFormatter = object : ValueFormatter() {
        private val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        override fun getFormattedValue(value: Float): String {
            val index = value.toInt()
            return if (index in sortedEntries.indices) dateFormat.format(Date(sortedEntries[index].date)) else ""
        }
    }

    val dataSet = LineDataSet(chartEntries, "Peso").apply {
        color = ContextCompat.getColor(context, R.color.gym_green)
        setCircleColor(ContextCompat.getColor(context, R.color.gym_green))
        lineWidth = 3f
        circleRadius = 5f
        setDrawCircleHole(true)
        valueTextSize = 10f
        setDrawFilled(true)
        fillDrawable = ContextCompat.getDrawable(context, R.drawable.chart_fill_gradient)
        mode = LineDataSet.Mode.CUBIC_BEZIER
        setDrawValues(true)
    }

    chart.data = LineData(dataSet)
    chart.setVisibleXRangeMaximum(5f)
    chart.moveViewToX(chartEntries.size.toFloat())
    chart.invalidate()
}

@Composable
private fun ProgressEntryCard(entry: ProgressEntry, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    ScreenCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val firstPhoto = entry.photoUri?.split(",")?.firstOrNull { it.isNotBlank() }
            if (!firstPhoto.isNullOrBlank()) {
                AsyncImage(
                    model = firstPhoto,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(dateFormat.format(Date(entry.date)), style = MaterialTheme.typography.titleMedium)
                Text("${entry.weightKg} kg")
                entry.bodyFatPercentage?.let { Text("$it% IMG") }
                if (!entry.notes.isNullOrBlank()) Text(entry.notes, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ProgressDetailDialog(
    entry: ProgressEntry,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-PT")) }
    val imageUrls = entry.photoUri?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Confirmar") },
            text = { Text("Queres mesmo eliminar este registo?") },
            confirmButton = { TextButton(onClick = { showConfirmDelete = false; onDelete() }) { Text("Sim") } },
            dismissButton = { TextButton(onClick = { showConfirmDelete = false }) { Text("Não") } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dateFormat.format(Date(entry.date))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(buildString {
                    append("${entry.weightKg} kg")
                    entry.bodyFatPercentage?.let { append("  |  ${it}% IMG") }
                })
                Text(entry.notes ?: "Sem observações.")
                if (imageUrls.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(imageUrls) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
        dismissButton = { TextButton(onClick = { showConfirmDelete = true }) { Text("Eliminar") } }
    )
}
