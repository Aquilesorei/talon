package com.aquilesorei.talon.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.aquilesorei.talon.data.local.entities.Measurement
import com.aquilesorei.talon.domain.models.UserProfile

object ExportUtils {
    
    suspend fun exportToCSV(context: Context, measurements: List<Measurement>): File = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "talon_export_$timestamp.csv"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        file.bufferedWriter().use { writer ->
            // Header
            writer.write("Date,Time,Weight(kg),BodyFat(%),Water(%),Muscle(kg),Impedance(Ω),BoneMass(kg),MetabolicAge,BMR(kcal),BMI,Notes\n")
            
            // Data rows
            measurements.forEach { m ->
                writer.write("${m.dateString()},${m.timeString()},${m.weight},${m.bodyFat},${m.water},${m.muscle},${m.impedance},${m.boneMass},${m.metabolicAge},${m.bmr},${m.bmi},\"${m.notes ?: ""}\"\n")
            }
        }
        
        file
    }
    
    suspend fun exportToJSON(context: Context, measurements: List<Measurement>, userProfile: UserProfile?): File = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "talon_backup_$timestamp.json"
        val file = File(context.getExternalFilesDir(null), fileName)
        
        val json = JSONObject().apply {
            put("version", 1)
            put("exportDate", System.currentTimeMillis())
            
            // User profile
            userProfile?.let {
                put("userProfile", JSONObject().apply {
                    put("heightCm", it.heightCm)
                    put("age", it.age)
                    put("isMale", it.isMale)
                    put("isAthlete", it.isAthlete)
                })
            }
            
            // Measurements
            put("measurements", JSONArray().apply {
                measurements.forEach { m ->
                    put(JSONObject().apply {
                        put("timestamp", m.timestamp)
                        put("weight", m.weight)
                        put("bodyFat", m.bodyFat)
                        put("water", m.water)
                        put("muscle", m.muscle)
                        put("impedance", m.impedance)
                        put("boneMass", m.boneMass)
                        put("metabolicAge", m.metabolicAge)
                        put("bmr", m.bmr)
                        put("bmi", m.bmi)
                        put("notes", m.notes ?: "")
                    })
                }
            })
        }
        
        file.writeText(json.toString(2))
        file
    }
    
    fun shareFile(context: Context, file: File, mimeType: String = "text/csv") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(intent, "Share export"))
    }
}
