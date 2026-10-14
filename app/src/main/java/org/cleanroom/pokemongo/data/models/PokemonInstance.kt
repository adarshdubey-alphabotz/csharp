package org.cleanroom.pokemongo.data.models

import java.util.UUID
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

data class Move(
    val name: String,
    val type: PokemonType,
    val power: Int,
    val isFast: Boolean = true
)

data class PokemonInstance(
    val id: String = UUID.randomUUID().toString(),
    val speciesId: Int,
    val nickname: String? = null,
    val level: Float = 15.0f,
    val ivAttack: Int = (0..15).random(),
    val ivDefense: Int = (0..15).random(),
    val ivStamina: Int = (0..15).random(),
    val fastMove: String = "Tackle",
    val chargedMove: String = "Struggle",
    val currentHp: Int = 100,
    val isFavorite: Boolean = false,
    val caughtTimestamp: Long = System.currentTimeMillis(),
    val heightM: Float = 1.0f,
    val weightKg: Float = 10.0f
) {
    // CPM (CP Multiplier) based on official Niantic progression (level 1 to 40)
    fun getCpMultiplier(): Float {
        return when {
            level <= 1.0f -> 0.094f
            level <= 10.0f -> 0.094f + (level - 1.0f) * 0.0365f
            level <= 20.0f -> 0.4225f + (level - 10.0f) * 0.0175f
            level <= 30.0f -> 0.5974f + (level - 20.0f) * 0.0134f
            else -> (0.7317f + (level - 30.0f) * 0.0058f).coerceAtMost(0.7903f)
        }
    }

    fun calculateMaxHp(species: PokemonSpecies): Int {
        val cpm = getCpMultiplier()
        val totalStamina = species.baseStamina + ivStamina
        return max(10, floor(totalStamina * cpm).toInt())
    }

    fun calculateCp(species: PokemonSpecies): Int {
        val cpm = getCpMultiplier()
        val totalAttack = species.baseAttack + ivAttack
        val totalDefense = species.baseDefense + ivDefense
        val totalStamina = species.baseStamina + ivStamina

        val rawCp = (totalAttack * sqrt(totalDefense.toDouble()) * sqrt(totalStamina.toDouble()) * (cpm * cpm)) / 10.0
        return max(10, floor(rawCp).toInt())
    }

    // Appraisal stars (0 to 3, or 4 for perfect 100% IV)
    fun getAppraisalStars(): Int {
        val totalIv = ivAttack + ivDefense + ivStamina // 0 to 45
        return when {
            totalIv == 45 -> 4 // Perfect 100% (Pink 3-star)
            totalIv >= 37 -> 3 // 82.2% - 97.8%
            totalIv >= 30 -> 2 // 66.7% - 80%
            totalIv >= 23 -> 1 // 51.1% - 64.4%
            else -> 0          // 0% - 48.9%
        }
    }
}
