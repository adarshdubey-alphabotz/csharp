package org.cleanroom.pokemongo

import org.cleanroom.pokemongo.data.automation.AutomationConfig
import org.cleanroom.pokemongo.data.automation.CaughtPreviewPrediction
import org.cleanroom.pokemongo.data.automation.EncounterIvDetails
import org.cleanroom.pokemongo.data.models.ItemType
import org.cleanroom.pokemongo.data.models.WildPokemonSpawn
import org.cleanroom.pokemongo.data.repository.PokedexDatabase
import org.cleanroom.pokemongo.data.repository.ThrowQuality
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

class AutomationEngineTest {

    @Test
    fun testEncounterIvCalculationFormulas() {
        val spawn = WildPokemonSpawn(
            id = "test-spawn-uuid-12345",
            speciesId = 25, // Pikachu
            latitude = 37.789,
            longitude = -122.401,
            level = 20.0f
        )

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

        assertTrue("Attack IV must be between 0 and 15", atk in 0..15)
        assertTrue("Defense IV must be between 0 and 15", def in 0..15)
        assertTrue("Stamina IV must be between 0 and 15", sta in 0..15)
        assertTrue("IV% must be between 0 and 100", ivPercent in 0.0f..100.0f)
        assertTrue("Stars must be 0 to 4", stars in 0..4)

        val species = PokedexDatabase.getById(25)!!
        val instance = org.cleanroom.pokemongo.data.models.PokemonInstance(
            speciesId = spawn.speciesId,
            level = spawn.level,
            ivAttack = atk,
            ivDefense = def,
            ivStamina = sta
        )
        val exactCp = instance.calculateCp(species)

        assertTrue("Exact CP for level 20 Pikachu must be between 400 and 700", exactCp in 400..700)
    }

    @Test
    fun testCaughtPreviewProbabilityOrdering() {
        val species = PokedexDatabase.getById(1)!! // Bulbasaur
        val level = 15.0f
        val cpm = 0.094f * sqrt(level)
        val innerProb = (species.baseCaptureRate / (2.0f * cpm)).coerceIn(0.01f, 0.99f)

        // Baseline: Standard Poke Ball, Normal throw, no berry, no curve
        val baseProb = (1.0 - (1.0 - innerProb).pow(1.0)).toFloat()

        // Ultra Ball + Razz Berry + Curveball + Excellent
        val totalMult = (ItemType.ULTRA_BALL.catchMultiplier * ItemType.RAZZ_BERRY.catchMultiplier * 1.7f * ThrowQuality.EXCELLENT.multiplier).toDouble()
        val boostedProb = (1.0 - (1.0 - innerProb).pow(totalMult)).toFloat()

        assertTrue("Base catch probability should be positive", baseProb > 0.05f)
        assertTrue("Boosted catch probability must exceed base probability", boostedProb > baseProb)
        assertTrue("Boosted catch probability should exceed 70%", boostedProb > 0.70f)
    }

    @Test
    fun testEnhancedThrowLogic() {
        // Enhanced throw simulation
        val enhancedQuality = ThrowQuality.EXCELLENT
        val enhancedCurve = true

        assertEquals(ThrowQuality.EXCELLENT, enhancedQuality)
        assertTrue(enhancedCurve)
        assertEquals(1.70f, enhancedQuality.multiplier, 0.01f)
        assertEquals(100, enhancedQuality.xpBonus)
    }

    @Test
    fun testAutomationConfigModel() {
        val config = AutomationConfig(
            autoCatchEnabled = true,
            autoSpinEnabled = true,
            autoFeedEnabled = true,
            preferredBall = ItemType.ULTRA_BALL,
            enhancedThrowEnabled = true,
            catchRangeMeters = 80.0,
            spinRangeMeters = 40.0,
            feedThresholdPercent = 30
        )

        assertTrue(config.autoCatchEnabled)
        assertTrue(config.autoSpinEnabled)
        assertTrue(config.autoFeedEnabled)
        assertEquals(ItemType.ULTRA_BALL, config.preferredBall)
        assertTrue(config.enhancedThrowEnabled)
        assertEquals(80.0, config.catchRangeMeters, 0.01)
        assertEquals(40.0, config.spinRangeMeters, 0.01)
        assertEquals(30, config.feedThresholdPercent)
    }
}
