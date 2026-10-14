package org.cleanroom.pokemongo.data.automation

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cleanroom.pokemongo.data.activation.ActivationManager
import org.cleanroom.pokemongo.data.activation.ProFeature
import org.cleanroom.pokemongo.data.models.ItemType
import org.cleanroom.pokemongo.data.models.PokemonInstance
import org.cleanroom.pokemongo.data.models.WildPokemonSpawn
import org.cleanroom.pokemongo.data.movement.GeoUtils
import org.cleanroom.pokemongo.data.repository.CatchResult
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.data.repository.PokedexDatabase
import org.cleanroom.pokemongo.data.repository.ThrowQuality
import kotlin.math.abs
import kotlin.math.pow

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
     * Resolves throw quality, curveball modifier, and AR+ bonus.
     * When ENHANCED_THROW is active and enabled, applies configured quality (Nice/Great/Excellent),
     * guaranteed curveball, and AR+ Expert Handler bonuses.
     */
    fun resolveThrow(userQuality: ThrowQuality, userCurveball: Boolean): Triple<ThrowQuality, Boolean, Boolean> {
        val isUnlocked = activationManager.isFeatureUnlocked(ProFeature.ENHANCED_THROW)
        if (isUnlocked && _config.value.enhancedThrowEnabled) {
            val targetQuality = _config.value.enhancedThrowQuality
            val curve = _config.value.guaranteedCurveball
            val arPlus = _config.value.arPlusBonusEnabled
            return Triple(targetQuality, curve, arPlus)
        }
        return Triple(userQuality, userCurveball, false)
    }

    /**
     * Filters encounter eligibility based on Block Non-Shiny and Block Non-Hundo rules.
     */
    fun checkEncounterAllowed(spawn: WildPokemonSpawn): EncounterFilterResult {
        val species = PokedexDatabase.getById(spawn.speciesId)?.name ?: "Pokémon"

        if (activationManager.isFeatureUnlocked(ProFeature.BLOCK_NON_SHINY) &&
            _config.value.blockNonShinyEnabled && !spawn.isShiny
        ) {
            return EncounterFilterResult.Blocked("Wild $species is not Shiny. Blocked by Block Non-Shiny filter!")
        }

        if (activationManager.isFeatureUnlocked(ProFeature.BLOCK_NON_HUNDO) &&
            _config.value.blockNonHundoEnabled && !spawn.isHundo
        ) {
            val ivText = String.format("%.1f%%", spawn.ivPercent)
            return EncounterFilterResult.Blocked("Wild $species IV is $ivText (<100%). Blocked by Block Non-Hundo filter!")
        }

        return EncounterFilterResult.Allowed
    }

    /**
     * Derives deterministic IV stats for wild encounters.
     */
    fun calculateEncounterIv(spawn: WildPokemonSpawn): EncounterIvDetails {
        val species = PokedexDatabase.getById(spawn.speciesId)
            ?: PokedexDatabase.speciesList.first()

        val atk = spawn.ivAttack
        val def = spawn.ivDefense
        val sta = spawn.ivStamina
        val ivPercent = spawn.ivPercent

        val stars = when {
            atk == 15 && def == 15 && sta == 15 -> 4
            ivPercent >= 82.2f -> 3
            ivPercent >= 66.7f -> 2
            ivPercent >= 50.0f -> 1
            else -> 0
        }

        val tempInstance = PokemonInstance(
            speciesId = spawn.speciesId,
            level = spawn.level,
            ivAttack = atk,
            ivDefense = def,
            ivStamina = sta,
            isShiny = spawn.isShiny
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

        val tempInstance = PokemonInstance(
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
     * Evaluates whether a newly caught Pokémon should be transferred automatically.
     */
    fun evaluateTransferOnCatch(instance: PokemonInstance): TransferOnCatchEvent? {
        if (!activationManager.isFeatureUnlocked(ProFeature.TRANSFER_ON_CATCH) || !_config.value.transferOnCatchEnabled) {
            return null
        }

        if (_config.value.keepShinyOnCatch && instance.isShiny) return null
        if (_config.value.keepHundoOnCatch && instance.isHundo) return null
        if (instance.isFavorite) return null
        if (_config.value.keepLegendaryOnCatch && (instance.speciesId in listOf(144, 145, 146, 150, 151))) return null
        if (_config.value.keepPvPRankOnCatch) {
            val glRank = org.cleanroom.pokemongo.data.pvp.PvpRankCalculator.calculateRank(instance, org.cleanroom.pokemongo.data.pvp.PvpLeague.GREAT_LEAGUE)
            val ulRank = org.cleanroom.pokemongo.data.pvp.PvpRankCalculator.calculateRank(instance, org.cleanroom.pokemongo.data.pvp.PvpLeague.ULTRA_LEAGUE)
            if (glRank.rank <= 50 || ulRank.rank <= 50) return null
        }

        if (instance.ivPercent < _config.value.transferIvThreshold) {
            val species = PokedexDatabase.getById(instance.speciesId)
            val speciesName = species?.name ?: "Pokémon"
            val cp = species?.let { instance.calculateCp(it) } ?: 100

            val transferred = repo.transferPokemon(instance.id)
            if (transferred) {
                return TransferOnCatchEvent(speciesName, cp, instance.ivPercent, 1)
            }
        }
        return null
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

        val nearbySpawns = repo.wildSpawns.value.filter { spawn ->
            GeoUtils.distanceMeters(playerLat, playerLon, spawn.latitude, spawn.longitude) <= range
        }
        if (nearbySpawns.isEmpty()) return null

        val target = nearbySpawns.first()

        // Respect encounter blocking filters
        val filterRes = checkEncounterAllowed(target)
        if (filterRes is EncounterFilterResult.Blocked) {
            return AutoCatchEvent.Escaped("${PokedexDatabase.getById(target.speciesId)?.name ?: "Pokémon"} (${filterRes.reason})")
        }

        val species = PokedexDatabase.getById(target.speciesId) ?: return null

        val items = repo.items.value
        val ballToUse = when {
            (items[_config.value.preferredBall] ?: 0) > 0 -> _config.value.preferredBall
            (items[ItemType.POKE_BALL] ?: 0) > 0 -> ItemType.POKE_BALL
            (items[ItemType.GREAT_BALL] ?: 0) > 0 -> ItemType.GREAT_BALL
            (items[ItemType.ULTRA_BALL] ?: 0) > 0 -> ItemType.ULTRA_BALL
            else -> return AutoCatchEvent.OutOfBalls
        }

        val (quality, isCurve, isAr) = resolveThrow(ThrowQuality.NORMAL, false)
        val result = repo.catchPokemon(
            spawn = target,
            ballType = ballToUse,
            berry = null,
            throwQuality = quality,
            isCurveball = isCurve,
            isArPlus = isAr
        )

        return when (result) {
            is CatchResult.Success -> {
                evaluateTransferOnCatch(result.pokemon)
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

        // Trigger Auto-Discard if enabled to clean surplus items
        if (_config.value.autoDiscardEnabled) {
            performAutoDiscard()
        }

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

    /**
     * Auto-Discard excess inventory items exceeding configured limits.
     */
    fun performAutoDiscard(): DiscardReport {
        if (!activationManager.isFeatureUnlocked(ProFeature.AUTO_DISCARD) || !_config.value.autoDiscardEnabled) {
            return DiscardReport(0, emptyMap())
        }

        val currentItems = repo.items.value
        val limits = _config.value.itemKeepLimits
        val discarded = mutableMapOf<ItemType, Int>()
        var total = 0

        for ((itemType, count) in currentItems) {
            val limit = limits[itemType] ?: Int.MAX_VALUE
            val excess = count - limit
            if (excess > 0) {
                repo.consumeItem(itemType, excess)
                discarded[itemType] = excess
                total += excess
            }
        }

        return DiscardReport(total, discarded)
    }

    /**
     * Auto-Incubator: Automatically assigns eggs to available incubators based on distance priority.
     */
    fun performAutoIncubation(): AutoIncubateResult {
        if (!activationManager.isFeatureUnlocked(ProFeature.AUTO_INCUBATOR) || !_config.value.autoIncubatorEnabled) {
            return AutoIncubateResult(0, emptyList())
        }

        val unincubatedEggs = if (_config.value.eggIncubatorPriority == "LONGEST_FIRST") {
            repo.eggs.value.filter { it.incubatorId == null && !it.isHatched }
                .sortedByDescending { it.tier.distanceKm }
        } else {
            repo.eggs.value.filter { it.incubatorId == null && !it.isHatched }
                .sortedBy { it.tier.distanceKm }
        }
        val availableIncubators = repo.incubators.value.filter { it.isAvailable() }

        if (unincubatedEggs.isEmpty() || availableIncubators.isEmpty()) {
            return AutoIncubateResult(0, emptyList())
        }

        val details = mutableListOf<String>()
        var assignedCount = 0

        val infiniteInc = availableIncubators.find { it.distanceMultiplier == 1.0f }
        val superIncs = availableIncubators.filter { it.distanceMultiplier < 1.0f }

        // Assign shortest distance egg to Infinite Incubator
        if (infiniteInc != null && unincubatedEggs.isNotEmpty()) {
            val shortestEgg = unincubatedEggs.first()
            if (repo.placeEggInIncubator(shortestEgg.id, infiniteInc.id)) {
                assignedCount++
                details.add("Assigned ${shortestEgg.tier.distanceKm}km Egg to Infinite Incubator")
            }
        }

        // Assign longest distance eggs to Super Incubators
        val remainingEggs = repo.eggs.value.filter { it.incubatorId == null && !it.isHatched }
            .sortedByDescending { it.tier.distanceKm }
        for ((idx, superInc) in superIncs.withIndex()) {
            if (idx < remainingEggs.size) {
                val egg = remainingEggs[idx]
                if (repo.placeEggInIncubator(egg.id, superInc.id)) {
                    assignedCount++
                    details.add("Assigned ${egg.tier.distanceKm}km Egg to Super Incubator")
                }
            }
        }

        return AutoIncubateResult(assignedCount, details)
    }

    /**
     * Gift Assistant: Automated friend gift batch opening and sending.
     */
    fun performGiftAssistant(): GiftAssistantResult {
        if (!activationManager.isFeatureUnlocked(ProFeature.GIFT_ASSISTANT) || !_config.value.giftAssistantEnabled) {
            return GiftAssistantResult(0, 0, 0, 0)
        }

        // Emulate batch gift opening (awards Stardust, Pokéballs, Potions) and sending
        val giftsOpened = 10
        val giftsSent = 10
        val stardust = 2000
        val xp = 2000

        repo.addRewards(xp.toLong(), stardust)
        repo.addItem(ItemType.POKE_BALL, 10)
        repo.addItem(ItemType.GREAT_BALL, 5)
        repo.addItem(ItemType.POTION, 5)

        return GiftAssistantResult(giftsOpened, giftsSent, stardust, xp)
    }

    /**
     * Quest Assistant: Automated field research task completion (snapshot & Pokestop interaction).
     */
    fun performQuestAssistant(): String? {
        if (!activationManager.isFeatureUnlocked(ProFeature.QUEST_ASSISTANT) || !_config.value.questAssistantEnabled) {
            return null
        }

        repo.addRewards(1500L, 1000)
        repo.addItem(ItemType.ULTRA_BALL, 3)
        return "Field Research Complete: Snapshots taken & rewards claimed (+1500 XP, +1000 Stardust, +3 Ultra Balls)!"
    }

    /**
     * Extended Buddy Care: Auto-petting and photo snapshot heart simulation.
     */
    fun performBuddyExtendedCare(): String? {
        if (!activationManager.isFeatureUnlocked(ProFeature.AUTO_FEED) || !_config.value.buddyAssistantExtended) {
            return null
        }

        val buddy = repo.buddy.value
        if (buddy.pokemonInstanceId == null) return null

        buddy.heartsPlay = 1
        buddy.heartsBattle = 1
        return "Buddy Care: Auto-played and took snapshot (+2 Affection Hearts)!"
    }


    private fun saveConfig(cfg: AutomationConfig) {
        prefs.edit()
            .putBoolean("auto_catch", cfg.autoCatchEnabled)
            .putBoolean("auto_spin", cfg.autoSpinEnabled)
            .putBoolean("auto_feed", cfg.autoFeedEnabled)
            .putString("preferred_ball", cfg.preferredBall.name)
            .putBoolean("enhanced_throw", cfg.enhancedThrowEnabled)
            .putString("enhanced_throw_quality", cfg.enhancedThrowQuality.name)
            .putBoolean("guaranteed_curve", cfg.guaranteedCurveball)
            .putBoolean("ar_plus_bonus", cfg.arPlusBonusEnabled)
            .putBoolean("encounter_iv", cfg.encounterIvEnabled)
            .putBoolean("caught_preview", cfg.caughtPreviewEnabled)
            .putBoolean("inventory_iv", cfg.inventoryIvBadgesEnabled)
            .putBoolean("joystick_enabled", cfg.joystickEnabled)
            .putBoolean("speed_controls_enabled", cfg.speedControlsEnabled)
            .putFloat("catch_range", cfg.catchRangeMeters.toFloat())
            .putFloat("spin_range", cfg.spinRangeMeters.toFloat())
            .putInt("feed_threshold", cfg.feedThresholdPercent)
            // Extended Mod Settings
            .putBoolean("block_non_shiny", cfg.blockNonShinyEnabled)
            .putBoolean("block_non_hundo", cfg.blockNonHundoEnabled)
            .putBoolean("quick_catch", cfg.quickCatchEnabled)
            .putBoolean("auto_discard", cfg.autoDiscardEnabled)
            .putBoolean("transfer_on_catch", cfg.transferOnCatchEnabled)
            .putFloat("transfer_iv_threshold", cfg.transferIvThreshold)
            .putBoolean("keep_shiny_catch", cfg.keepShinyOnCatch)
            .putBoolean("keep_hundo_catch", cfg.keepHundoOnCatch)
            .putBoolean("keep_legendary_catch", cfg.keepLegendaryOnCatch)
            .putBoolean("keep_pvp_rank_catch", cfg.keepPvPRankOnCatch)
            .putBoolean("auto_incubator", cfg.autoIncubatorEnabled)
            .putString("egg_incubator_priority", cfg.eggIncubatorPriority)
            .putBoolean("gift_assistant", cfg.giftAssistantEnabled)
            .putBoolean("quest_assistant", cfg.questAssistantEnabled)
            .putBoolean("buddy_extended", cfg.buddyAssistantExtended)
            .putInt("spawn_multiplier", cfg.spawnBoosterMultiplier)
            .putBoolean("hide_trainer_name", cfg.hideTrainerName)
            .putBoolean("hide_app_gesture", cfg.hideAppWithGestureEnabled)
            .putString("custom_shortcuts", cfg.customShortcuts.joinToString(","))
            .putString("app_language", cfg.appLanguage)
            .putString("pokemon_feed_language", cfg.pokemonFeedLanguage)
            .putBoolean("gym_assist", cfg.gymBattleAssistEnabled)
            .putBoolean("dynamax_assist", cfg.dynamaxAssistEnabled)
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
            enhancedThrowQuality = try {
                ThrowQuality.valueOf(prefs.getString("enhanced_throw_quality", ThrowQuality.EXCELLENT.name) ?: ThrowQuality.EXCELLENT.name)
            } catch (e: Exception) {
                ThrowQuality.EXCELLENT
            },
            guaranteedCurveball = prefs.getBoolean("guaranteed_curve", true),
            arPlusBonusEnabled = prefs.getBoolean("ar_plus_bonus", false),
            encounterIvEnabled = prefs.getBoolean("encounter_iv", true),
            caughtPreviewEnabled = prefs.getBoolean("caught_preview", true),
            inventoryIvBadgesEnabled = prefs.getBoolean("inventory_iv", true),
            joystickEnabled = prefs.getBoolean("joystick_enabled", true),
            speedControlsEnabled = prefs.getBoolean("speed_controls_enabled", true),
            catchRangeMeters = prefs.getFloat("catch_range", 80.0f).toDouble(),
            spinRangeMeters = prefs.getFloat("spin_range", 40.0f).toDouble(),
            feedThresholdPercent = prefs.getInt("feed_threshold", 30),
            // Extended Mod Settings
            blockNonShinyEnabled = prefs.getBoolean("block_non_shiny", false),
            blockNonHundoEnabled = prefs.getBoolean("block_non_hundo", false),
            quickCatchEnabled = prefs.getBoolean("quick_catch", false),
            autoDiscardEnabled = prefs.getBoolean("auto_discard", false),
            transferOnCatchEnabled = prefs.getBoolean("transfer_on_catch", false),
            transferIvThreshold = prefs.getFloat("transfer_iv_threshold", 90.0f),
            keepShinyOnCatch = prefs.getBoolean("keep_shiny_catch", true),
            keepHundoOnCatch = prefs.getBoolean("keep_hundo_catch", true),
            keepLegendaryOnCatch = prefs.getBoolean("keep_legendary_catch", true),
            keepPvPRankOnCatch = prefs.getBoolean("keep_pvp_rank_catch", true),
            autoIncubatorEnabled = prefs.getBoolean("auto_incubator", false),
            eggIncubatorPriority = prefs.getString("egg_incubator_priority", "SHORTEST_FIRST") ?: "SHORTEST_FIRST",
            giftAssistantEnabled = prefs.getBoolean("gift_assistant", false),
            questAssistantEnabled = prefs.getBoolean("quest_assistant", false),
            buddyAssistantExtended = prefs.getBoolean("buddy_extended", false),
            spawnBoosterMultiplier = prefs.getInt("spawn_multiplier", 1),
            hideTrainerName = prefs.getBoolean("hide_trainer_name", false),
            hideAppWithGestureEnabled = prefs.getBoolean("hide_app_gesture", true),
            customShortcuts = (prefs.getString("custom_shortcuts", null)?.split(",")?.filter { it.isNotBlank() })
                ?: listOf("SNIPER", "ROUTES", "TOOLS", "HEAL", "SPAWNS"),
            appLanguage = prefs.getString("app_language", "English") ?: "English",
            pokemonFeedLanguage = prefs.getString("pokemon_feed_language", "English") ?: "English",
            gymBattleAssistEnabled = prefs.getBoolean("gym_assist", false),
            dynamaxAssistEnabled = prefs.getBoolean("dynamax_assist", false)
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
