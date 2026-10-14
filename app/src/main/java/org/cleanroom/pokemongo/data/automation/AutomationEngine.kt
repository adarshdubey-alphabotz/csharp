package org.cleanroom.pokemongo.data.automation

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cleanroom.pokemongo.data.activation.ActivationManager
import org.cleanroom.pokemongo.data.activation.ProFeature
import org.cleanroom.pokemongo.data.models.ItemType
import org.cleanroom.pokemongo.data.models.WildPokemonSpawn
import org.cleanroom.pokemongo.data.movement.GeoUtils
import org.cleanroom.pokemongo.data.repository.CatchResult
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.data.repository.PokedexDatabase
import org.cleanroom.pokemongo.data.repository.ThrowQuality
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

class AutomationEngine(
    context: Context,
    private val repo: GameStateRepository,
    private val activationManager: ActivationManager
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cleanroom_automation_prefs", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<AutomationConfig> = _config.asStateFlow()

    fun updateConfig(newConfig: AutomationConfig) {
        _config.value = newConfig
        saveConfig(newConfig)
    }

    /**
     * Resolves throw quality and curveball modifier.
     * When ENHANCED_THROW is active and enabled, guarantees 100% EXCELLENT and Curveball.
     */
    fun resolveThrow(userQuality: ThrowQuality, userCurveball: Boolean): Pair<ThrowQuality, Boolean> {
        val isUnlocked = activationManager.isFeatureUnlocked(ProFeature.ENHANCED_THROW)
        if (isUnlocked && _config.value.enhancedThrowEnabled) {
            return Pair(ThrowQuality.EXCELLENT, true)
        }
        return Pair(userQuality, userCurveball)
    }

    /**
     * Derives deterministic IV stats for wild encounters.
     */
    fun calculateEncounterIv(spawn: WildPokemonSpawn): EncounterIvDetails {
        val species = PokedexDatabase.getById(spawn.speciesId)
            ?: PokedexDatabase.speciesList.first()

        val seed = abs(spawn.id.hashCode())
        val atk = seed % 16
        val def = (seed / 16) % 16
        val sta = (seed / 256) % 16
        val ivPercent = ((atk + def + sta) / 45.0f) * 100.0f

        val stars = when {
            atk == 15 && def == 15 && sta == 15 -> 4
            ivPercent >= 82.2f -> 3
            ivPercent >= 66.7f -> 2
            ivPercent >= 50.0f -> 1
            else -> 0
        }

        // Exact CP at spawn level using shared formula
        val tempInstance = org.cleanroom.pokemongo.data.models.PokemonInstance(
            speciesId = spawn.speciesId,
            level = spawn.level,
            ivAttack = atk,
            ivDefense = def,
            ivStamina = sta
        )
        val exactCp = tempInstance.calculateCp(species)

        return EncounterIvDetails(
            ivAttack = atk,
            ivDefense = def,
            ivStamina = sta,
            ivPercent = ivPercent,
            stars = stars,
            exactCp = exactCp
        )
    }

    /**
     * Predicts catch outcome probability for Caught Preview.
     */
    fun predictCatchOutcome(
        spawn: WildPokemonSpawn,
        ballType: ItemType,
        berry: ItemType?,
        quality: ThrowQuality,
        isCurveball: Boolean
    ): CaughtPreviewPrediction {
        val species = PokedexDatabase.getById(spawn.speciesId)
            ?: PokedexDatabase.speciesList.first()

        val tempInstance = org.cleanroom.pokemongo.data.models.PokemonInstance(
            speciesId = spawn.speciesId,
            level = spawn.level
        )
        val cpm = tempInstance.getCpMultiplier()
        val innerProb = (species.baseCaptureRate / (2.0f * cpm)).coerceIn(0.01f, 0.99f)

        val ballMult = ballType.catchMultiplier
        val berryMult = berry?.catchMultiplier ?: 1.0f
        val curveMult = if (isCurveball) 1.7f else 1.0f
        val qualityMult = quality.multiplier

        val totalMult = (ballMult * berryMult * curveMult * qualityMult).toDouble()
        val catchProb = (1.0 - (1.0 - innerProb).pow(totalMult)).toFloat().coerceIn(0.01f, 0.999f)

        val isLikely = catchProb >= 0.50f
        val percentText = (catchProb * 100.0f).let { String.format("%.1f%%", it) }
        val message = if (isLikely) "High Probability ($percentText)" else "Low Probability ($percentText)"

        return CaughtPreviewPrediction(
            isLikelyCaught = isLikely,
            probability = catchProb,
            message = message
        )
    }

    /**
     * Executes one tick of Auto-Catch for nearby spawns within range.
     */
    fun performAutoCatchTick(): AutoCatchEvent? {
        if (!activationManager.isFeatureUnlocked(ProFeature.AUTO_CATCH) || !_config.value.autoCatchEnabled) {
            return null
        }

        val playerLat = repo.playerLatitude.value
        val playerLon = repo.playerLongitude.value
        val range = _config.value.catchRangeMeters

        // Filter spawns within configured range
        val nearbySpawns = repo.wildSpawns.value.filter { spawn ->
            GeoUtils.distanceMeters(playerLat, playerLon, spawn.latitude, spawn.longitude) <= range
        }
        if (nearbySpawns.isEmpty()) return null

        val target = nearbySpawns.first()
        val species = PokedexDatabase.getById(target.speciesId) ?: return null

        // Determine ball to use
        val items = repo.items.value
        val ballToUse = when {
            (items[_config.value.preferredBall] ?: 0) > 0 -> _config.value.preferredBall
            (items[ItemType.POKE_BALL] ?: 0) > 0 -> ItemType.POKE_BALL
            (items[ItemType.GREAT_BALL] ?: 0) > 0 -> ItemType.GREAT_BALL
            (items[ItemType.ULTRA_BALL] ?: 0) > 0 -> ItemType.ULTRA_BALL
            else -> return AutoCatchEvent.OutOfBalls
        }

        val (quality, isCurve) = resolveThrow(ThrowQuality.NORMAL, false)
        val result = repo.catchPokemon(
            spawn = target,
            ballType = ballToUse,
            berry = null,
            throwQuality = quality,
            isCurveball = isCurve
        )

        return when (result) {
            is CatchResult.Success -> {
                val cp = result.pokemon.calculateCp(species)
                AutoCatchEvent.Captured(species.name, cp, ballToUse)
            }
            is CatchResult.Fled, is CatchResult.Breakout -> {
                AutoCatchEvent.Escaped(species.name)
            }
        }
    }

    /**
     * Executes one tick of Auto-Spin for nearby PokéStops within range.
     */
    fun performAutoSpinTick(): AutoSpinEvent? {
        if (!activationManager.isFeatureUnlocked(ProFeature.AUTO_SPIN) || !_config.value.autoSpinEnabled) {
            return null
        }

        val playerLat = repo.playerLatitude.value
        val playerLon = repo.playerLongitude.value
        val range = _config.value.spinRangeMeters

        val readyStops = repo.pokestops.value.filter { stop ->
            !stop.isInCooldown() && GeoUtils.distanceMeters(playerLat, playerLon, stop.latitude, stop.longitude) <= range
        }
        if (readyStops.isEmpty()) return null

        val targetStop = readyStops.first()
        val spinResult = repo.spinPokestop(targetStop.id) ?: return null

        val itemsCount = spinResult.itemsAwarded.sumOf { it.second }
        return AutoSpinEvent.Spun(spinResult.pokestopName, itemsCount, spinResult.xpAwarded)
    }

    /**
     * Executes one tick of Auto-Feed for active Buddy when hunger is low.
     */
    fun performAutoFeedTick(): AutoFeedEvent? {
        if (!activationManager.isFeatureUnlocked(ProFeature.AUTO_FEED) || !_config.value.autoFeedEnabled) {
            return null
        }

        val currentBuddy = repo.buddy.value
        if (currentBuddy.pokemonInstanceId == null) return null
        if (currentBuddy.hungerPercent >= _config.value.feedThresholdPercent) return null

        val items = repo.items.value
        val berryToUse = when {
            (items[ItemType.RAZZ_BERRY] ?: 0) > 0 -> ItemType.RAZZ_BERRY
            (items[ItemType.NANAB_BERRY] ?: 0) > 0 -> ItemType.NANAB_BERRY
            (items[ItemType.PINAP_BERRY] ?: 0) > 0 -> ItemType.PINAP_BERRY
            else -> return null
        }

        val success = repo.feedBuddy(berryToUse)
        if (success) {
            return AutoFeedEvent.Fed(berryToUse, repo.buddy.value.hungerPercent)
        }
        return null
    }

    private fun saveConfig(cfg: AutomationConfig) {
        prefs.edit()
            .putBoolean("auto_catch", cfg.autoCatchEnabled)
            .putBoolean("auto_spin", cfg.autoSpinEnabled)
            .putBoolean("auto_feed", cfg.autoFeedEnabled)
            .putString("preferred_ball", cfg.preferredBall.name)
            .putBoolean("enhanced_throw", cfg.enhancedThrowEnabled)
            .putBoolean("guaranteed_curve", cfg.guaranteedCurveball)
            .putBoolean("encounter_iv", cfg.encounterIvEnabled)
            .putBoolean("caught_preview", cfg.caughtPreviewEnabled)
            .putBoolean("inventory_iv", cfg.inventoryIvBadgesEnabled)
            .putBoolean("joystick_enabled", cfg.joystickEnabled)
            .putBoolean("speed_controls_enabled", cfg.speedControlsEnabled)
            .putFloat("catch_range", cfg.catchRangeMeters.toFloat())
            .putFloat("spin_range", cfg.spinRangeMeters.toFloat())
            .putInt("feed_threshold", cfg.feedThresholdPercent)
            .apply()
    }

    private fun loadConfig(): AutomationConfig {
        return AutomationConfig(
            autoCatchEnabled = prefs.getBoolean("auto_catch", false),
            autoSpinEnabled = prefs.getBoolean("auto_spin", false),
            autoFeedEnabled = prefs.getBoolean("auto_feed", false),
            preferredBall = try {
                ItemType.valueOf(prefs.getString("preferred_ball", ItemType.POKE_BALL.name) ?: ItemType.POKE_BALL.name)
            } catch (e: Exception) {
                ItemType.POKE_BALL
            },
            enhancedThrowEnabled = prefs.getBoolean("enhanced_throw", false),
            guaranteedCurveball = prefs.getBoolean("guaranteed_curve", true),
            encounterIvEnabled = prefs.getBoolean("encounter_iv", true),
            caughtPreviewEnabled = prefs.getBoolean("caught_preview", true),
            inventoryIvBadgesEnabled = prefs.getBoolean("inventory_iv", true),
            joystickEnabled = prefs.getBoolean("joystick_enabled", true),
            speedControlsEnabled = prefs.getBoolean("speed_controls_enabled", true),
            catchRangeMeters = prefs.getFloat("catch_range", 80.0f).toDouble(),
            spinRangeMeters = prefs.getFloat("spin_range", 40.0f).toDouble(),
            feedThresholdPercent = prefs.getInt("feed_threshold", 30)
        )
    }

    companion object {
        @Volatile
        private var instance: AutomationEngine? = null

        fun getInstance(
            context: Context,
            repo: GameStateRepository,
            activationManager: ActivationManager
        ): AutomationEngine {
            return instance ?: synchronized(this) {
                instance ?: AutomationEngine(
                    context.applicationContext,
                    repo,
                    activationManager
                ).also { instance = it }
            }
        }
    }
}
