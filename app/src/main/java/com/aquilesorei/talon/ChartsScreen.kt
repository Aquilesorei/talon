package com.aquilesorei.talon

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

enum class TimeRange(val label: String, val days: Int) {
    WEEK("Week", 7),
    MONTH("Month", 30),
    THREE_MONTHS("3 Months", 90),
    YEAR("Year", 365),
    ALL("All", Int.MAX_VALUE)
}

@Composable
fun ChartsScreen() {
    val context = LocalContext.current
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(context))
    val measurements by viewModel.historyList.collectAsState()
    
    var selectedRange by remember { mutableStateOf(TimeRange.MONTH) }
    var selectedMetric by remember { mutableStateOf("weight") }
    
    // Filter measurements by time range
    val filteredMeasurements = remember(measurements, selectedRange) {
        val cutoffTime = System.currentTimeMillis() - (selectedRange.days * 24 * 60 * 60 * 1000L)
        measurements.filter { it.timestamp >= cutoffTime }.reversed() // Oldest first for charts
    }
    
    // Calculate statistics
    val stats = remember(filteredMeasurements, selectedMetric) {
        if (filteredMeasurements.isEmpty()) {
            mapOf("avg" to 0f, "min" to 0f, "max" to 0f, "change" to 0f)
        } else {
            val values = filteredMeasurements.map { measurement ->
                when (selectedMetric) {
                    "weight" -> measurement.weight
                    "bodyFat" -> measurement.bodyFat
                    "muscle" -> measurement.muscle
                    "water" -> measurement.water
                    else -> measurement.weight
                }
            }
            val avg = values.average().toFloat()
            val min = values.minOrNull() ?: 0f
            val max = values.maxOrNull() ?: 0f
            val change = if (values.size > 1) values.last() - values.first() else 0f
            mapOf("avg" to avg, "min" to min, "max" to max, "change" to change)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Progress Charts",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Time Range Selector
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Time Range", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeRange.entries.forEach { range ->
                        FilterChip(
                            selected = selectedRange == range,
                            onClick = { selectedRange = range },
                            label = { Text(range.label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Metric Selector
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Metric", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "weight" to "Weight",
                        "bodyFat" to "Body Fat",
                        "muscle" to "Muscle",
                        "water" to "Water"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedMetric == key,
                            onClick = { selectedMetric = key },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Statistics Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Average",
                value = formatValue(stats["avg"] ?: 0f, selectedMetric),
                icon = Icons.Default.TrendingFlat,
                color = Color(0xFF64B5F6)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Change",
                value = formatChange(stats["change"] ?: 0f, selectedMetric),
                icon = if ((stats["change"] ?: 0f) >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                color = if ((stats["change"] ?: 0f) >= 0) Color(0xFF81C784) else Color(0xFFE57373)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Min",
                value = formatValue(stats["min"] ?: 0f, selectedMetric),
                icon = Icons.Default.ArrowDownward,
                color = Color(0xFFFFD54F)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Max",
                value = formatValue(stats["max"] ?: 0f, selectedMetric),
                icon = Icons.Default.ArrowUpward,
                color = Color(0xFFFF7043)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Chart
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${getMetricLabel(selectedMetric)} Trend",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (filteredMeasurements.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No data for selected time range",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    TrendChart(
                        measurements = filteredMeasurements,
                        metric = selectedMetric,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun TrendChart(
    measurements: List<Measurement>,
    metric: String,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    
    LaunchedEffect(measurements, metric) {
        val dataPoints = measurements.map { measurement ->
            when (metric) {
                "weight" -> measurement.weight
                "bodyFat" -> measurement.bodyFat
                "muscle" -> measurement.muscle
                "water" -> measurement.water
                else -> measurement.weight
            }
        }
        
        modelProducer.runTransaction {
            lineSeries { series(dataPoints) }
        }
    }
    
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis()
        ),
        modelProducer = modelProducer,
        modifier = modifier
    )
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun formatValue(value: Float, metric: String): String {
    return when (metric) {
        "weight" -> String.format("%.1f kg", value)
        "bodyFat", "water" -> String.format("%.1f%%", value)
        "muscle" -> String.format("%.1f kg", value)
        else -> String.format("%.1f", value)
    }
}

fun formatChange(value: Float, metric: String): String {
    val sign = if (value >= 0) "+" else ""
    return when (metric) {
        "weight" -> String.format("%s%.1f kg", sign, value)
        "bodyFat", "water" -> String.format("%s%.1f%%", sign, value)
        "muscle" -> String.format("%s%.1f kg", sign, value)
        else -> String.format("%s%.1f", sign, value)
    }
}

fun getMetricLabel(metric: String): String {
    return when (metric) {
        "weight" -> "Weight"
        "bodyFat" -> "Body Fat %"
        "muscle" -> "Muscle Mass"
        "water" -> "Water %"
        else -> "Weight"
    }
}
