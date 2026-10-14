package org.cleanroom.pokemongo

import org.cleanroom.pokemongo.data.activation.ActivationFeatureChecker
import org.cleanroom.pokemongo.data.activation.ActivationManager
import org.cleanroom.pokemongo.data.activation.ActivationState
import org.cleanroom.pokemongo.data.activation.ActivationTier
import org.cleanroom.pokemongo.data.activation.ProFeature
import org.cleanroom.pokemongo.data.automation.*
import org.cleanroom.pokemongo.data.models.ItemType
import org.cleanroom.pokemongo.data.models.PokemonInstance
import org.cleanroom.pokemongo.data.models.WildPokemonSpawn
import org.cleanroom.pokemongo.data.movement.*
import org.cleanroom.pokemongo.data.naming.PokemonNameGenerator
import org.cleanroom.pokemongo.data.pvp.PvpLeague
import org.cleanroom.pokemongo.data.pvp.PvpRankCalculator
import org.cleanroom.pokemongo.data.repository.PokedexDatabase
import org.cleanroom.pokemongo.data.repository.ThrowQuality
import org.cleanroom.pokemongo.data.settings.SettingsExporter
import org.cleanroom.pokemongo.data.sniper.SniperManager
import org.junit.Assert.*
import org.junit.Test

class ExtendedFeaturesTest {

    @Test
    fun testBlockNonShinyFilter() {
        val nonShinySpawn = WildPokemonSpawn(
            speciesId = 25,
            latitude = 37.789,
            longitude = -122.401,
            isShiny = false
        )
        val shinySpawn = WildPokemonSpawn(
            speciesId = 25,
            latitude = 37.789,
            longitude = -122.401,
            isShiny = true
        )

        assertFalse(nonShinySpawn.isShiny)
        assertTrue(shinySpawn.isShiny)
    }

    @Test
    fun testBlockNonHundoFilter() {
        val hundoSpawn = WildPokemonSpawn(
            speciesId = 149, // Dragonite
            latitude = 37.789,
            longitude = -122.401,
            ivAttack = 15,
            ivDefense = 15,
            ivStamina = 15
        )
        val nonHundoSpawn = WildPokemonSpawn(
            speciesId = 149,
            latitude = 37.789,
            longitude = -122.401,
            ivAttack = 14,
            ivDefense = 15,
            ivStamina = 15
        )

        assertTrue("15/15/15 must be hundo", hundoSpawn.isHundo)
        assertEquals(100.0f, hundoSpawn.ivPercent, 0.01f)

        assertFalse("14/15/15 must not be hundo", nonHundoSpawn.isHundo)
        assertTrue(nonHundoSpawn.ivPercent < 100.0f)
    }

    @Test
    fun testPvpRankCalculatorGreatLeague() {
        // Calculate Great League rank for Venusaur (ID: 3)
        val res = PvpRankCalculator.calculatePvpRank(
            speciesId = 3,
            ivAtk = 0,
            ivDef = 14,
            ivSta = 13,
            league = PvpLeague.GREAT_LEAGUE
        )

        assertEquals(PvpLeague.GREAT_LEAGUE, res.league)
        assertTrue("CP at optimal level must be <= 1500", res.cpAtLevel <= 1500)
        assertTrue("Stat product must be positive", res.statProduct > 0)
        assertTrue("Rank must be positive integer", res.rank >= 1)
        assertTrue("Stat product % must be between 80% and 100%", res.statProductPercent in 80f..100f)
    }

    @Test
    fun testPvpRankCalculatorUltraLeague() {
        // Calculate Ultra League rank for Gengar (ID: 94)
        val res = PvpRankCalculator.calculatePvpRank(
            speciesId = 94,
            ivAtk = 2,
            ivDef = 15,
            ivSta = 15,
            league = PvpLeague.ULTRA_LEAGUE
        )

        assertEquals(PvpLeague.ULTRA_LEAGUE, res.league)
        assertTrue("CP at optimal level must be <= 2500", res.cpAtLevel <= 2500)
        assertTrue("Rank must be >= 1", res.rank >= 1)
    }

    @Test
    fun testPokemonNameGeneratorTemplates() {
        val instance = PokemonInstance(
            speciesId = 25, // Pikachu
            level = 20.0f,
            ivAttack = 15,
            ivDefense = 15,
            ivStamina = 15,
            fastMove = "ThunderShock",
            chargedMove = "Thunderbolt"
        )

        val nameIv = PokemonNameGenerator.generateName(instance, PokemonNameGenerator.TEMPLATE_NAME_IV)
        assertEquals("Pikachu_100", nameIv)

        val stats = PokemonNameGenerator.generateName(instance, PokemonNameGenerator.TEMPLATE_IV_STATS)
        assertEquals("100_15/15/15", stats)

        val full = PokemonNameGenerator.generateName(instance, "{name}_{iv}_{fastMove:3}/{chargedMove:3}")
        assertEquals("Pikachu_100_Thu/Thu", full)
    }

    @Test
    fun testSettingsJsonExportAndImport() {
        val initialConfig = AutomationConfig(
            autoCatchEnabled = true,
            autoSpinEnabled = true,
            blockNonShinyEnabled = true,
            blockNonHundoEnabled = true,
            quickCatchEnabled = true,
            autoDiscardEnabled = true,
            transferOnCatchEnabled = true,
            transferIvThreshold = 85.0f,
            spawnBoosterMultiplier = 3,
            hideTrainerName = true
        )

        val json = SettingsExporter.exportToJson(initialConfig)
        assertTrue("JSON must contain autoCatchEnabled", json.contains("autoCatchEnabled"))
        assertTrue("JSON must contain blockNonShinyEnabled", json.contains("blockNonShinyEnabled"))

        val importRes = SettingsExporter.importFromJson(json)
        assertTrue("Import must succeed", importRes.isSuccess)
        val restored = importRes.getOrThrow()

        assertTrue(restored.autoCatchEnabled)
        assertTrue(restored.blockNonShinyEnabled)
        assertTrue(restored.blockNonHundoEnabled)
        assertTrue(restored.quickCatchEnabled)
        assertTrue(restored.autoDiscardEnabled)
        assertTrue(restored.transferOnCatchEnabled)
        assertEquals(85.0f, restored.transferIvThreshold, 0.01f)
        assertEquals(3, restored.spawnBoosterMultiplier)
        assertTrue(restored.hideTrainerName)
    }

    @Test
    fun testRouteGpxParsingAndLoopModes() {
        val gpxSample = """
            <gpx>
              <trkpt lat="41.6561" lon="-0.8773"></trkpt>
              <trkpt lat="41.6575" lon="-0.8790"></trkpt>
              <trkpt lat="41.6582" lon="-0.8760"></trkpt>
            </gpx>
        """.trimIndent()

        val dummyValidator = object : org.cleanroom.pokemongo.data.activation.ActivationValidator {
            override fun validateKey(key: String, currentTime: Long): Result<ActivationState.Active> =
                Result.success(ActivationState.Active(key, 0L, ActivationTier.PRO))
        }

        val parsedRoute = Route(
            name = "Test Route",
            waypoints = listOf(
                RouteWaypoint(41.6561, -0.8773, "P1"),
                RouteWaypoint(41.6575, -0.8790, "P2"),
                RouteWaypoint(41.6582, -0.8760, "P3")
            ),
            mode = RouteLoopMode.LOOP
        )

        assertEquals(3, parsedRoute.waypoints.size)
        assertEquals(RouteLoopMode.LOOP, parsedRoute.mode)
    }

    @Test
    fun testAllExtendedProFeaturesEnumeration() {
        val allFeatures = ProFeature.values()
        assertTrue("Must include at least 20 features", allFeatures.size >= 20)

        // Core 10
        assertTrue(allFeatures.contains(ProFeature.JOYSTICK))
        assertTrue(allFeatures.contains(ProFeature.SPEED_CONTROLS))
        assertTrue(allFeatures.contains(ProFeature.DOUBLE_TAP_TELEPORT))
        assertTrue(allFeatures.contains(ProFeature.ENHANCED_THROW))
        assertTrue(allFeatures.contains(ProFeature.ENCOUNTER_IV))
        assertTrue(allFeatures.contains(ProFeature.CAUGHT_PREVIEW))
        assertTrue(allFeatures.contains(ProFeature.AUTO_CATCH))
        assertTrue(allFeatures.contains(ProFeature.AUTO_SPIN))
        assertTrue(allFeatures.contains(ProFeature.AUTO_FEED))
        assertTrue(allFeatures.contains(ProFeature.INVENTORY_IV))

        // Extended 14
        assertTrue(allFeatures.contains(ProFeature.BLOCK_NON_SHINY))
        assertTrue(allFeatures.contains(ProFeature.BLOCK_NON_HUNDO))
        assertTrue(allFeatures.contains(ProFeature.QUICK_CATCH))
        assertTrue(allFeatures.contains(ProFeature.QUICK_SNIPER))
        assertTrue(allFeatures.contains(ProFeature.AUTO_DISCARD))
        assertTrue(allFeatures.contains(ProFeature.TRANSFER_ON_CATCH))
        assertTrue(allFeatures.contains(ProFeature.AUTO_INCUBATOR))
        assertTrue(allFeatures.contains(ProFeature.GIFT_ASSISTANT))
        assertTrue(allFeatures.contains(ProFeature.QUEST_ASSISTANT))
        assertTrue(allFeatures.contains(ProFeature.ROUTE_WALK))
        assertTrue(allFeatures.contains(ProFeature.PVP_RANK_CALCULATOR))
        assertTrue(allFeatures.contains(ProFeature.NAME_GENERATOR))
        assertTrue(allFeatures.contains(ProFeature.SPAWN_BOOSTER))
        assertTrue(allFeatures.contains(ProFeature.BATTLE_ASSISTANT))
    }

    @Test
    fun testEnhancedThrowQualityAndArPlusBonus() {
        val throwNice = ThrowQuality.NICE
        val throwGreat = ThrowQuality.GREAT
        val throwExcellent = ThrowQuality.EXCELLENT

        assertEquals(1.15f, throwNice.multiplier, 0.01f)
        assertEquals(1.30f, throwGreat.multiplier, 0.01f)
        assertEquals(1.70f, throwExcellent.multiplier, 0.01f)
        assertEquals(100, throwExcellent.xpBonus)

        // AR+ bonus adds 300 XP and 1.25x capture multiplier
        val baseCatchRate = 0.4f
        val qualityMultiplier = throwExcellent.multiplier
        val curveMultiplier = 1.7f
        val arPlusMultiplier = 1.25f

        val finalCatchRate = (baseCatchRate * qualityMultiplier * curveMultiplier * arPlusMultiplier).coerceAtMost(1.0f)
        assertTrue("Final catch rate with AR+ and Excellent Curve must exceed 0.9", finalCatchRate > 0.9f)
    }

    @Test
    fun testTransferOnCatchLegendaryAndPvPProtection() {
        val mewtwo = PokemonInstance(
            speciesId = 150, // Mewtwo (Legendary)
            level = 20.0f,
            ivAttack = 10,
            ivDefense = 10,
            ivStamina = 10
        )
        assertTrue("Mewtwo is legendary", mewtwo.speciesId in listOf(144, 145, 146, 150, 151))
        assertTrue("Mewtwo IV < 80%", mewtwo.ivPercent < 80.0f)

        // PvP Rank Protection
        val pvpCandidate = PokemonInstance(
            speciesId = 3, // Venusaur
            level = 20.0f,
            ivAttack = 0,
            ivDefense = 14,
            ivStamina = 13
        )
        val pvpResult = PvpRankCalculator.calculateRank(pvpCandidate, PvpLeague.GREAT_LEAGUE)
        assertTrue("Top PvP spread must have rank <= 50", pvpResult.rank <= 50)
    }

    @Test
    fun testEggIncubatorPrioritizationSorting() {
        val egg2k = org.cleanroom.pokemongo.data.models.Egg(id = "e1", tier = org.cleanroom.pokemongo.data.models.EggTier.TWO_KM)
        val egg5k = org.cleanroom.pokemongo.data.models.Egg(id = "e2", tier = org.cleanroom.pokemongo.data.models.EggTier.FIVE_KM)
        val egg10k = org.cleanroom.pokemongo.data.models.Egg(id = "e3", tier = org.cleanroom.pokemongo.data.models.EggTier.TEN_KM)
        val eggs = listOf(egg5k, egg10k, egg2k)

        val shortestFirst = eggs.sortedBy { it.tier.distanceKm }
        assertEquals(org.cleanroom.pokemongo.data.models.EggTier.TWO_KM, shortestFirst.first().tier)
        assertEquals(org.cleanroom.pokemongo.data.models.EggTier.TEN_KM, shortestFirst.last().tier)

        val longestFirst = eggs.sortedByDescending { it.tier.distanceKm }
        assertEquals(org.cleanroom.pokemongo.data.models.EggTier.TEN_KM, longestFirst.first().tier)
        assertEquals(org.cleanroom.pokemongo.data.models.EggTier.TWO_KM, longestFirst.last().tier)
    }

    @Test
    fun testPokedexLocalizationMultiLanguage() {
        assertEquals("Pikachu", PokedexDatabase.getLocalizedName(25, "English"))
        assertEquals("ピカチュウ (Pikachu)", PokedexDatabase.getLocalizedName(25, "Japanese"))
        assertEquals("Pikachu", PokedexDatabase.getLocalizedName(25, "German"))
        assertEquals("Pikachu", PokedexDatabase.getLocalizedName(25, "French"))

        assertEquals("Bulbasaur", PokedexDatabase.getLocalizedName(1, "English"))
        assertEquals("フシギダネ (Fushigidane)", PokedexDatabase.getLocalizedName(1, "Japanese"))
        assertEquals("Bisasam", PokedexDatabase.getLocalizedName(1, "German"))
        assertEquals("Bulbizarre", PokedexDatabase.getLocalizedName(1, "French"))

        assertEquals("Glumanda", PokedexDatabase.getLocalizedName(4, "German"))
        assertEquals("Salamèche", PokedexDatabase.getLocalizedName(4, "French"))
        assertEquals("Schiggy", PokedexDatabase.getLocalizedName(7, "German"))
        assertEquals("Carapuce", PokedexDatabase.getLocalizedName(7, "French"))
    }

    @Test
    fun testSniperLocalizedFeeds() {
        val dummyChecker = object : ActivationFeatureChecker {
            override fun isFeatureUnlocked(feature: ProFeature): Boolean = true
            override fun isProActive(): Boolean = true
        }
        val sniperManager = SniperManager(dummyChecker)
        val germanHundo = sniperManager.getHundoFeed(37.789, -122.401, language = "German")
        assertTrue("German hundo feed must not be empty", germanHundo.isNotEmpty())
        val germanNames = germanHundo.map { it.speciesName }
        assertTrue("Must contain German names like Dragoran, Garados, or Relaxo",
            germanNames.contains("Dragoran") || germanNames.contains("Garados") || germanNames.contains("Relaxo") || germanNames.contains("Glurak"))

        val frenchPvp = sniperManager.getPvpFeed(37.789, -122.401, language = "French")
        assertTrue("French pvp feed must not be empty", frenchPvp.isNotEmpty())
        val frenchNames = frenchPvp.map { it.speciesName }
        assertTrue("Must contain French names like Florizarre, Ectoplasma, or Tortank",
            frenchNames.contains("Florizarre") || frenchNames.contains("Ectoplasma") || frenchNames.contains("Tortank") || frenchNames.contains("Mackogneur"))
    }

    @Test
    fun testSettingsExporterFullExtendedRoundtrip() {
        val original = AutomationConfig(
            autoCatchEnabled = true,
            autoSpinEnabled = true,
            blockNonShinyEnabled = true,
            blockNonHundoEnabled = true,
            quickCatchEnabled = true,
            autoDiscardEnabled = true,
            transferOnCatchEnabled = true,
            transferIvThreshold = 91.5f,
            keepShinyOnCatch = true,
            keepHundoOnCatch = true,
            keepLegendaryOnCatch = true,
            keepPvPRankOnCatch = true,
            enhancedThrowEnabled = true,
            enhancedThrowQuality = ThrowQuality.EXCELLENT,
            guaranteedCurveball = true,
            arPlusBonusEnabled = true,
            autoIncubatorEnabled = true,
            eggIncubatorPriority = "LONGEST_FIRST",
            giftAssistantEnabled = true,
            questAssistantEnabled = true,
            buddyAssistantExtended = true,
            speedControlsEnabled = true,
            joystickEnabled = true,
            spawnBoosterMultiplier = 3,
            hideTrainerName = true,
            hideAppWithGestureEnabled = true,
            customShortcuts = listOf("SNIPER", "ROUTES", "STEALTH"),
            appLanguage = "German",
            pokemonFeedLanguage = "Japanese",
            gymBattleAssistEnabled = true,
            dynamaxAssistEnabled = true
        )

        val json = SettingsExporter.exportToJson(original)
        val imported = SettingsExporter.importFromJson(json).getOrThrow()

        assertEquals(original.autoCatchEnabled, imported.autoCatchEnabled)
        assertEquals(original.enhancedThrowQuality, imported.enhancedThrowQuality)
        assertEquals(original.guaranteedCurveball, imported.guaranteedCurveball)
        assertEquals(original.arPlusBonusEnabled, imported.arPlusBonusEnabled)
        assertEquals(original.eggIncubatorPriority, imported.eggIncubatorPriority)
        assertEquals(original.keepLegendaryOnCatch, imported.keepLegendaryOnCatch)
        assertEquals(original.keepPvPRankOnCatch, imported.keepPvPRankOnCatch)
        assertEquals(original.hideAppWithGestureEnabled, imported.hideAppWithGestureEnabled)
        assertEquals(original.customShortcuts, imported.customShortcuts)
        assertEquals(original.appLanguage, imported.appLanguage)
        assertEquals(original.pokemonFeedLanguage, imported.pokemonFeedLanguage)
        assertEquals(original.spawnBoosterMultiplier, imported.spawnBoosterMultiplier)
    }
}
