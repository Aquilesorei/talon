package com.aquilesorei.talon

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class FAQItem(
    val question: String,
    val answer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen() {
    val faqs = listOf(
        FAQItem(
            "How do I connect my smart scale?",
            "Go to the Measure tab, tap the scan button, and select your scale from the list. Make sure Bluetooth is enabled and you're standing on the scale."
        ),
        FAQItem(
            "What is BMI?",
            "Body Mass Index (BMI) is a measure of body fat based on height and weight. Normal range is 18.5-24.9. It's a general indicator but doesn't account for muscle mass."
        ),
        FAQItem(
            "What is body fat percentage?",
            "Body fat percentage is the proportion of fat in your body. Healthy ranges: Men 14-18%, Women 21-25%. Athletes typically have lower percentages."
        ),
        FAQItem(
            "What is BMR?",
            "Basal Metabolic Rate (BMR) is the number of calories your body burns at rest. It helps determine daily calorie needs for weight management."
        ),
        FAQItem(
            "What is metabolic age?",
            "Metabolic age compares your BMR to the average for your actual age. A lower metabolic age indicates better metabolic health."
        ),
        FAQItem(
            "How accurate are the measurements?",
            "Bioimpedance scales are generally accurate for tracking trends. For best results: measure at the same time daily, avoid measuring after exercise or eating, and ensure bare feet."
        ),
        FAQItem(
            "Can I export my data?",
            "Yes! Go to Settings → Data Management. You can export to CSV for spreadsheets or create a JSON backup for full data portability."
        ),
        FAQItem(
            "How do goals work?",
            "Create goals in the Goals tab. Set target values for weight, body fat, or muscle mass. The app tracks your progress and celebrates when you achieve your goals."
        ),
        FAQItem(
            "What does impedance mean?",
            "Impedance is the electrical resistance measured by the scale. Higher impedance typically indicates more body fat, lower indicates more muscle/water."
        ),
        FAQItem(
            "Why is my data different from yesterday?",
            "Body composition fluctuates daily due to hydration, food intake, exercise, and time of day. Focus on weekly trends rather than daily changes."
        )
    )
    
    var expandedIndex by remember { mutableStateOf<Int?>(null) }
    var showMetricInfo by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Help & FAQ",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Quick Tips Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row {
                    Icon(Icons.Default.Lightbulb, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Tips for Accurate Measurements", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("• Measure at the same time each day", style = MaterialTheme.typography.bodyMedium)
                Text("• Use bare feet on the scale", style = MaterialTheme.typography.bodyMedium)
                Text("• Avoid measuring after exercise", style = MaterialTheme.typography.bodyMedium)
                Text("• Stay hydrated consistently", style = MaterialTheme.typography.bodyMedium)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Metric Explanations Button
        OutlinedButton(
            onClick = { showMetricInfo = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Info, null)
            Spacer(Modifier.width(8.dp))
            Text("View Metric Explanations")
        }
        
        Spacer(Modifier.height(24.dp))
        
        Text("Frequently Asked Questions", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        
        // FAQ Accordion
        faqs.forEachIndexed { index, faq ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { expandedIndex = if (expandedIndex == index) null else index }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            faq.question,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (expandedIndex == index) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                    
                    if (expandedIndex == index) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            faq.answer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    // Metric Info Dialog
    if (showMetricInfo) {
        AlertDialog(
            onDismissRequest = { showMetricInfo = false },
            title = { Text("Body Metrics Explained") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    MetricExplanation("Weight", "Total body mass in kilograms")
                    MetricExplanation("BMI", "Body Mass Index: weight/height². Normal: 18.5-24.9")
                    MetricExplanation("Body Fat %", "Percentage of body weight that is fat. Healthy: M 14-18%, F 21-25%")
                    MetricExplanation("Water %", "Body water percentage. Healthy: 50-65%")
                    MetricExplanation("Muscle Mass", "Total muscle weight in kilograms")
                    MetricExplanation("Bone Mass", "Estimated bone weight, typically 3-5% of body weight")
                    MetricExplanation("BMR", "Calories burned at rest per day")
                    MetricExplanation("Metabolic Age", "Metabolic health compared to chronological age")
                }
            },
            confirmButton = {
                TextButton(onClick = { showMetricInfo = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
fun MetricExplanation(title: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
