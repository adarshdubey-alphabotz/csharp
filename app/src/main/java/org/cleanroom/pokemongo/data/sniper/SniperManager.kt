package org.cleanroom.pokemongo.data.sniper

import org.cleanroom.pokemongo.data.activation.ActivationManager
import org.cleanroom.pokemongo.data.activation.ProFeature
import org.cleanroom.pokemongo.data.movement.GeoUtils
import org.cleanroom.pokemongo.data.movement.MovementController
import org.cleanroom.pokemongo.data.repository.PokedexDatabase
import java.util.UUID

enum class SniperFeedType(val displayName: String) {
    HUNDO_100IV("Quick Sniper I (100% IV Hundo)"),
    TOP_PVP_RANKS("Quick Sniper II (Top PvP Ranks)")
}

data class SniperTarget(
    val id: String = UUID.randomUUID().toString(),
    val speciesId: Int,
    val speciesName: String,
    val latitude: Double,
    val longitude: Double,
    val cp: Int,
    val level: Float,
    val ivAttack: Int,
    val ivDefense: Int,
    val ivStamina: Int,
    val ivPercent: Float,
    val league: String? = null,
    val pvpRank: Int? = null,
    val despawnTimestamp: Long = System.currentTimeMillis() + (12 * 60 * 1000L),
    val cityName: String
) {
    fun remainingMinutes(): Int {
        val diff = despawnTimestamp - System.currentTimeMillis()
        return ((diff / 1000L) / 60).toInt().coerceAtLeast(0)
    }
}

class SniperManager(
    private val activationManager: org.cleanroom.pokemongo.data.activation.ActivationFeatureChecker
) {
    // Curated global hotspot targets
    private val hotspots = listOf(
        Pair("San Francisco (Pier 39)", Pair(37.8087, -122.4098)),
        Pair("New York (Central Park)", Pair(40.7829, -73.9654)),
        Pair("Tokyo (Shinjuku)", Pair(35.6938, 139.7034)),
        Pair("Zaragoza (Spain)", Pair(41.6561, -0.8773)),
        Pair("Sydney (Circular Quay)", Pair(-33.8614, 151.2108)),
        Pair("London (Hyde Park)", Pair(51.5073, -0.1657)),
        Pair("Paris (Eiffel Tower)", Pair(48.8584, 2.2945))
    )

    fun getHundoFeed(playerLat: Double, playerLon: Double, language: String = "English"): List<SniperTarget> {
        if (!activationManager.isFeatureUnlocked(ProFeature.QUICK_SNIPER)) {
            return emptyList()
        }

        val hundoSpeciesIds = listOf(149, 130, 143, 6, 9, 3, 94, 68, 59, 131, 144, 145, 146, 150) // Gen 1 powerhouses
        return hotspots.mapIndexed { index, (city, coords) ->
            val speciesId = hundoSpeciesIds[index % hundoSpeciesIds.size]
            val species = PokedexDatabase.getById(speciesId) ?: PokedexDatabase.speciesList.first()
            val localizedName = PokedexDatabase.getLocalizedName(species.id, language)
            val level = 30.0f
            val tempInstance = org.cleanroom.pokemongo.data.models.PokemonInstance(
                speciesId = species.id,
                level = level,
                ivAttack = 15,
                ivDefense = 15,
                ivStamina = 15
            )
            val cp = tempInstance.calculateCp(species)

            SniperTarget(
                speciesId = species.id,
                speciesName = localizedName,
                latitude = coords.first,
                longitude = coords.second,
                cp = cp,
                level = level,
                ivAttack = 15,
                ivDefense = 15,
                ivStamina = 15,
                ivPercent = 100.0f,
                cityName = city
            )
        }
    }

    fun getPvpFeed(playerLat: Double, playerLon: Double, language: String = "English"): List<SniperTarget> {
        if (!activationManager.isFeatureUnlocked(ProFeature.QUICK_SNIPER)) {
            return emptyList()
        }

        val pvpSpecies = listOf(
            Triple(131, "Lapras", "Great League"),
            Triple(94, "Gengar", "Ultra League"),
            Triple(3, "Venusaur", "Great League"),
            Triple(9, "Blastoise", "Ultra League"),
            Triple(68, "Machamp", "Great League"),
            Triple(143, "Snorlax", "Ultra League")
        )

        return pvpSpecies.mapIndexed { index, (speciesId, _, league) ->
            val (city, coords) = hotspots[index % hotspots.size]
            val species = PokedexDatabase.getById(speciesId) ?: PokedexDatabase.speciesList.first()
            val localizedName = PokedexDatabase.getLocalizedName(species.id, language)
            val level = if (league == "Great League") 20.0f else 28.0f
            // Optimal PvP IV: Low Attack, max Defense & Stamina
            val atk = 1
            val def = 15
            val sta = 15
            val tempInstance = org.cleanroom.pokemongo.data.models.PokemonInstance(
                speciesId = species.id,
                level = level,
                ivAttack = atk,
                ivDefense = def,
                ivStamina = sta
            )
            val cp = tempInstance.calculateCp(species)
            val ivPercent = ((atk + def + sta) / 45.0f) * 100.0f

            SniperTarget(
                speciesId = species.id,
                speciesName = localizedName,
                latitude = coords.first,
                longitude = coords.second,
                cp = cp,
                level = level,
                ivAttack = atk,
                ivDefense = def,
                ivStamina = sta,
                ivPercent = ivPercent,
                league = league,
                pvpRank = 1,
                cityName = city
            )
        }
    }

    fun teleportToTarget(target: SniperTarget, movementController: MovementController) {
        movementController.teleportTo(target.latitude, target.longitude)
    }
}
