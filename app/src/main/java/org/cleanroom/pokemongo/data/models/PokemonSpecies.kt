package org.cleanroom.pokemongo.data.models

enum class PokemonType(val displayName: String, val hexColor: Long) {
    NORMAL("Normal", 0xFFA8A878),
    FIRE("Fire", 0xFFF08030),
    WATER("Water", 0xFF6890F0),
    GRASS("Grass", 0xFF78C850),
    ELECTRIC("Electric", 0xFFF8D030),
    ICE("Ice", 0xFF98D8D8),
    FIGHTING("Fighting", 0xFFC03028),
    POISON("Poison", 0xFFA040A0),
    GROUND("Ground", 0xFFE0C068),
    FLYING("Flying", 0xFFA890F0),
    PSYCHIC("Psychic", 0xFFF85888),
    BUG("Bug", 0xFFA8B820),
    ROCK("Rock", 0xFFB8A038),
    GHOST("Ghost", 0xFF705898),
    DRAGON("Dragon", 0xFF7038F8),
    STEEL("Steel", 0xFFB8B8D0),
    FAIRY("Fairy", 0xFFEE99AC)
}

data class PokemonSpecies(
    val id: Int,
    val name: String,
    val primaryType: PokemonType,
    val secondaryType: PokemonType? = null,
    val baseAttack: Int,
    val baseDefense: Int,
    val baseStamina: Int,
    val baseCaptureRate: Float = 0.20f,
    val baseFleeRate: Float = 0.10f,
    val candyToEvolve: Int = 0,
    val evolvesToSpeciesId: Int? = null,
    val heightM: Float,
    val weightKg: Float,
    val flavorText: String,
    val buddyKmPerCandy: Float = 3.0f
)
