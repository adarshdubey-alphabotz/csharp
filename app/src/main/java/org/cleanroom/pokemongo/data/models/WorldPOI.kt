package org.cleanroom.pokemongo.data.models

import java.util.UUID

enum class Team(val displayName: String, val hexColor: Long) {
    NONE("No Team", 0xFF9E9E9E),
    MYSTIC("Team Mystic", 0xFF0288D1),
    VALOR("Team Valor", 0xFFD32F2F),
    INSTINCT("Team Instinct", 0xFFFBC02D)
}

data class Pokestop(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    var lastSpunTimestamp: Long = 0L,
    var hasLure: Boolean = false
) {
    fun isInCooldown(currentTime: Long = System.currentTimeMillis()): Boolean {
        return (currentTime - lastSpunTimestamp) < (5 * 60 * 1000L) // 5 minutes cooldown
    }

    fun remainingCooldownSeconds(currentTime: Long = System.currentTimeMillis()): Long {
        val diff = (5 * 60 * 1000L) - (currentTime - lastSpunTimestamp)
        return (diff / 1000L).coerceAtLeast(0L)
    }
}

data class Gym(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    var team: Team = Team.MYSTIC,
    val defenderSpeciesName: String = "Dragonite",
    val defenderCp: Int = 3120
)

data class WildPokemonSpawn(
    val id: String = UUID.randomUUID().toString(),
    val speciesId: Int,
    val latitude: Double,
    val longitude: Double,
    val level: Float = (1..30).random().toFloat(),
    val ivAttack: Int = (0..15).random(),
    val ivDefense: Int = (0..15).random(),
    val ivStamina: Int = (0..15).random(),
    val despawnTimestamp: Long = System.currentTimeMillis() + (15 * 60 * 1000L) // 15 mins
)
