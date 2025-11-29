package com.aquilesorei.talon.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.pow

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Singleton : un seul utilisateur
    val heightCm: Int,
    val age: Int,
    val isMale: Boolean,
    val isAthlete: Boolean = false
)

enum class BMICategory(val label: String, val color: Long) {
    UNDERWEIGHT("Underweight", 0xFF64B5F6), // Bleu clair
    NORMAL("Normal", 0xFF81C784),      // Vert
    OVERWEIGHT("Overweight", 0xFFFFD54F),   // Jaune/Orange
    OBESE("Obese", 0xFFE57373)       // Rouge
}

data class BodyComposition(
    val bmi: Float,
    val bodyFatPercentage: Float,
    val waterPercentage: Float,
    val muscleMass: Float,
    val boneMass: Float,
    val metabolicAge: Int,
    val bmr: Int // Basal Metabolic Rate
) {
    fun getBMICategory(): BMICategory = when {
        bmi < 18.5f -> BMICategory.UNDERWEIGHT
        bmi < 25f -> BMICategory.NORMAL
        bmi < 30f -> BMICategory.OVERWEIGHT
        else -> BMICategory.OBESE
    }

    fun getBodyFatCategory(isMale: Boolean): String = when {
        isMale -> when {
            bodyFatPercentage < 6f -> "Essential"
            bodyFatPercentage < 14f -> "Athletic"
            bodyFatPercentage < 18f -> "Fitness"
            bodyFatPercentage < 25f -> "Average"
            else -> "Obese"
        }
        else -> when {
            bodyFatPercentage < 14f -> "Essential"
            bodyFatPercentage < 21f -> "Athletic"
            bodyFatPercentage < 25f -> "Fitness"
            bodyFatPercentage < 32f -> "Average"
            else -> "Obese"
        }
    }

    fun getHealthyBodyFatRange(isMale: Boolean): String =
        if (isMale) "14-18%" else "21-25%"

    fun getHealthyWaterRange(): String = "50-65%"
}

object BodyMetricsCalculator {

    // --- FORMULE TALON / NHANES (1999-2004) ---
    // Coefficients calculés par Machine Learning sur 60,000 profils
    private const val COEFF_H2R = -0.76086f    // Height² / Impedance
    private const val COEFF_WEIGHT = 0.49385f  // Poids
    private const val COEFF_AGE = -0.00686f    // Age
    private const val COEFF_MALE = -5.28771f   // Sexe (1=H, 0=F)
    private const val CONSTANT = 31.72188f     // Base

    fun calculate(weightKg: Float, impedance: Float, user: UserProfile): BodyComposition {
        // 1. Calcul IMC (BMI)
        val heightM = user.heightCm / 100f
        val bmi = if (heightM > 0) weightKg / (heightM * heightM) else 0f

        // Si données invalides (chaussettes ou erreur), on retourne vide
        if (impedance <= 0 || weightKg <= 0 || user.heightCm <= 0) {
            return BodyComposition(bmi, 0f, 0f, 0f, 0f, 0, 0)
        }

        // 2. Calcul du Body Fat % (Formule Scientifique)
        // Variable physique : H² / R (Volume conducteur)
        val h2r = (user.heightCm * user.heightCm) / impedance
        val isMaleVal = if (user.isMale) 1f else 0f

        var bodyFat = (h2r * COEFF_H2R) +
                (weightKg * COEFF_WEIGHT) +
                (user.age * COEFF_AGE) +
                (isMaleVal * COEFF_MALE) +
                CONSTANT

        // Ajustement Athlète (Les tissus musculaires denses faussent la BIA standard)
        if (user.isAthlete) {
            bodyFat *= 0.85f // Correction de 15%
        }

        // Bornes physiologiques (Sécurité)
        bodyFat = bodyFat.coerceIn(3f, 70f)

        // 3. Eau (Water %)
        // Le tissu maigre (LBM) contient ~73% d'eau. Le gras ~10%.
        // On utilise une approximation robuste pour l'affichage.
        val waterPercentage = (100f - bodyFat) * 0.72f

        // 4. Masse Musculaire (Muscle Mass)
        // LBM = Poids Total - Poids du Gras
        val fatMass = weightKg * (bodyFat / 100f)
        val leanBodyMass = weightKg - fatMass
        // On affiche souvent la LBM comme "Muscle" sur les balances grand public,
        // ou LBM - Os. Ici, on prend LBM pour être cohérent avec OKOK.
        val muscleMass = leanBodyMass

        // 5. Masse Osseuse
        // Estimation RTC standard
        val boneRate = if (user.isMale) 0.042f else 0.036f
        val boneMass = weightKg * boneRate

        // 6. BMR (Mifflin-St Jeor)
        val bmrBase = (10f * weightKg) + (6.25f * user.heightCm) - (5f * user.age)
        val bmr = if (user.isMale) (bmrBase + 5).toInt() else (bmrBase - 161).toInt()

        // 7. Âge Métabolique
        // Comparaison du BMR utilisateur vs BMR standard pour son âge
        // "Si tu brûles autant qu'un jeune de 20 ans, tu as métaboliquement 20 ans"
        // Formule simplifiée pour l'UX
        val idealBmr25 = (10f * weightKg) + (6.25f * user.heightCm) - (5f * 25) + (if (user.isMale) 5 else -161)
        val metabolicAge = if (bmr > idealBmr25) {
            // Meilleur métabolisme que la moyenne -> plus jeune
            (user.age - (bmr - idealBmr25) / 20).toInt().coerceAtLeast(18)
        } else {
            // Moins bon -> plus vieux
            (user.age + (idealBmr25 - bmr) / 20).toInt().coerceAtMost(80)
        }

        return BodyComposition(
            bmi = bmi,
            bodyFatPercentage = bodyFat,
            waterPercentage = waterPercentage,
            muscleMass = muscleMass,
            boneMass = boneMass,
            metabolicAge = metabolicAge,
            bmr = bmr
        )
    }
}