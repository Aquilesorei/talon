package com.aquilesorei.talon

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.pow

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Always 1 since there's only one user profile
    val heightCm: Int,
    val age: Int,
    val isMale: Boolean,
    val isAthlete: Boolean = false // Les athlètes ont une densité musculaire différente
)

enum class BMICategory(val label: String, val color: Long) {
    UNDERWEIGHT("Underweight", 0xFF64B5F6),
    NORMAL("Normal", 0xFF81C784),
    OVERWEIGHT("Overweight", 0xFFFFD54F),
    OBESE("Obese", 0xFFE57373)
}

data class BodyComposition(
    val bmi: Float,
    val bodyFatPercentage: Float,
    val waterPercentage: Float,
    val muscleMass: Float,
    val boneMass: Float,
    val metabolicAge: Int,
    val bmr: Int // Basal Metabolic Rate (Calories/jour)
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
            else -> "Above Average"
        }
        else -> when {
            bodyFatPercentage < 14f -> "Essential"
            bodyFatPercentage < 21f -> "Athletic"
            bodyFatPercentage < 25f -> "Fitness"
            bodyFatPercentage < 32f -> "Average"
            else -> "Above Average"
        }
    }
    
    fun getHealthyBodyFatRange(isMale: Boolean): String =
        if (isMale) "14-18%" else "21-25%"
    
    fun getHealthyWaterRange(): String = "50-65%"
}

object BodyMetricsCalculator {

    fun calculate(weightKg: Float, impedance: Float, user: UserProfile): BodyComposition {
        // 1. Calcul IMC (BMI) standard
        // Formule : Poids / Taille(m)²
        val heightM = user.heightCm / 100f
        val bmi = if (heightM > 0) weightKg / (heightM * heightM) else 0f

        // Si l'impédance est 0 (ex: chaussettes), on ne peut calculer que l'IMC
        if (impedance <= 0) {
            return BodyComposition(bmi, 0f, 0f, 0f, 0f, 0, 0)
        }

        // 2. Calcul de la LBM (Lean Body Mass - Masse Maigre)
        // Cette formule est une approximation courante pour la BIA (Bio-Impedance Analysis)
        // LBM = Coeff * Hauteur² / Impédance + Coeff * Poids + ...

        // Coefficients (peuvent varier légèrement selon les constructeurs, ceux-ci sont standards)
        val lbmCoefficient = if (user.isMale) 0.32810 else 0.32810 // Souvent identique pour la base
        val weightCoeff = 0.33929
        val ageCoeff = 0.29569

        // Formule simplifiée dérivée de Deurenberg & al. pour l'estimation grand public
        // Note: Pour un vrai projet open source, on peut affiner ces constantes.

        // Calcul théorique du Body Fat (Formule de Deurenberg adaptée BIA)
        // BF% = (1.20 * BMI) + (0.23 * Age) - (10.8 * Sexe) - 5.4
        // Sexe : 1 pour Homme, 0 pour Femme
        val sexFactor = if (user.isMale) 1 else 0
        var bodyFat = (1.20f * bmi) + (0.23f * user.age) - (10.8f * sexFactor) - 5.4f

        // Correction par l'impédance (La "Magie" de la balance)
        // Si l'impédance est élevée = moins d'eau = plus de gras.
        // On ajuste le résultat théorique avec la mesure réelle.
        // C'est une simplification linéaire pour l'exemple :
        val impedanceFactor = (impedance - 500) * 0.01f
        bodyFat += impedanceFactor

        // Bornes de sécurité (pour ne pas afficher -5% ou 120%)
        bodyFat = bodyFat.coerceIn(5f, 70f)

        // 3. Eau (Water %)
        // Le gras contient peu d'eau (~10%), le muscle beaucoup (~75%).
        // Relation inverse au Body Fat.
        val waterPercentage = (100f - bodyFat) * 0.7f // Approximation standard

        // 4. Masse Musculaire
        val muscleMass = weightKg * ((100f - bodyFat) / 100f)

        // 5. Masse Osseuse (Est. RTC - Randomized Control Trial tables)
        // En général ~3-5% du poids pour les hommes, ~2.5-4% pour les femmes
        val boneRate = if (user.isMale) 0.045f else 0.035f
        val boneMass = weightKg * boneRate

        // 6. BMR (Basal Metabolic Rate) - Formule de Mifflin-St Jeor
        // Hommes : (10 × poids) + (6.25 × taille) - (5 × âge) + 5
        // Femmes : (10 × poids) + (6.25 × taille) - (5 × âge) - 161
        val bmrBase = (10f * weightKg) + (6.25f * user.heightCm) - (5f * user.age)
        val bmr = if (user.isMale) (bmrBase + 5).toInt() else (bmrBase - 161).toInt()

        // 7. Âge Métabolique
        // Si BMR > BMR standard pour ton âge, tu es "plus jeune".
        // Simplification : On compare ton BMR à une moyenne.
        val metabolicAge = if (bmr > bmrBase) user.age - 2 else user.age + 2 // Algo très basique ici

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