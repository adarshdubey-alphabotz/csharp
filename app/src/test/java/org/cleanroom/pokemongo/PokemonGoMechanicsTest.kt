package org.cleanroom.pokemongo

import org.cleanroom.pokemongo.data.models.*
import org.cleanroom.pokemongo.data.repository.PokedexDatabase
import org.cleanroom.pokemongo.data.repository.ThrowQuality
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.pow

class PokemonGoMechanicsTest {

    @Test
    fun testPokedexRosterIntegrity() {
        val bulbasaur = PokedexDatabase.getById(1)
        assertNotNull("Bulbasaur should exist in Pokedex", bulbasaur)
        assertEquals("Bulbasaur", bulbasaur?.name)
        assertEquals(PokemonType.GRASS, bulbasaur?.primaryType)
        assertEquals(PokemonType.POISON, bulbasaur?.secondaryType)
        assertEquals(2, bulbasaur?.evolvesToSpeciesId)
        assertEquals(25, bulbasaur?.candyToEvolve)

        val pikachu = PokedexDatabase.getById(25)
        assertNotNull("Pikachu should exist", pikachu)
        assertEquals("Pikachu", pikachu?.name)
        assertEquals(PokemonType.ELECTRIC, pikachu?.primaryType)
        assertEquals(26, pikachu?.evolvesToSpeciesId)
    }

    @Test
    fun testCpCalculationFormula() {
        val species = PokedexDatabase.getById(25)!! // Pikachu
        val pokemon = PokemonInstance(
            speciesId = 25,
            level = 20.0f,
            ivAttack = 15,
            ivDefense = 15,
            ivStamina = 15
        )

        val cp = pokemon.calculateCp(species)
        assertTrue("Level 20 100% IV Pikachu CP should be between 500 and 650", cp in 500..650)

        val hp = pokemon.calculateMaxHp(species)
        assertTrue("Max HP should be greater than 60", hp > 60)
    }

    @Test
    fun testAppraisalEvaluation() {
        val perfectPokemon = PokemonInstance(
            speciesId = 1,
            ivAttack = 15,
            ivDefense = 15,
            ivStamina = 15
        )
        assertEquals("Perfect 45/45 IVs should evaluate to 4 stars", 4, perfectPokemon.getAppraisalStars())

        val decentPokemon = PokemonInstance(
            speciesId = 1,
            ivAttack = 10,
            ivDefense = 10,
            ivStamina = 10
        )
        assertEquals("30/45 IVs should evaluate to 2 stars", 2, decentPokemon.getAppraisalStars())

        val lowPokemon = PokemonInstance(
            speciesId = 1,
            ivAttack = 5,
            ivDefense = 5,
            ivStamina = 5
        )
        assertEquals("15/45 IVs should evaluate to 0 stars", 0, lowPokemon.getAppraisalStars())
    }

    @Test
    fun testCatchProbabilityMultipliers() {
        val baseCaptureRate = 0.20f
        val cpm = 0.45f
        val innerProb = (baseCaptureRate / (2.0f * cpm)).coerceIn(0.01f, 0.99f)

        // Normal throw with standard Poke Ball
        val normalChance = 1.0 - (1.0 - innerProb).pow(1.0)

        // Excellent Curveball with Ultra Ball & Razz Berry
        val ultraMultiplier = ItemType.ULTRA_BALL.catchMultiplier // 2.0
        val razzMultiplier = ItemType.RAZZ_BERRY.catchMultiplier // 1.5
        val curveMultiplier = 1.7f
        val excellentMultiplier = ThrowQuality.EXCELLENT.multiplier // 1.7
        val totalMultiplier = (ultraMultiplier * razzMultiplier * curveMultiplier * excellentMultiplier).toDouble()

        val boostedChance = 1.0 - (1.0 - innerProb).pow(totalMultiplier)

        assertTrue("Boosted throw must significantly exceed standard throw chance", boostedChance > normalChance * 2)
        assertTrue("Boosted catch chance should be above 70%", boostedChance > 0.70)
    }

    @Test
    fun testEggDistanceAndIncubationLogic() {
        val egg = Egg(tier = EggTier.FIVE_KM, walkedKm = 4.8f)
        assertFalse("Egg at 4.8 / 5.0 km should not be ready to hatch", egg.isReadyToHatch())

        egg.walkedKm += 0.3f
        assertTrue("Egg at 5.1 / 5.0 km must be ready to hatch", egg.isReadyToHatch())
    }

    @Test
    fun testPokestopCooldown() {
        val stop = Pokestop(
            name = "Test Stop",
            latitude = 37.78,
            longitude = -122.40,
            lastSpunTimestamp = System.currentTimeMillis() - (2 * 60 * 1000L) // 2 mins ago
        )
        assertTrue("Stop spun 2 mins ago should be in cooldown", stop.isInCooldown())
        assertTrue("Remaining cooldown should be around 180s", stop.remainingCooldownSeconds() in 150..200)

        val expiredStop = Pokestop(
            name = "Expired Stop",
            latitude = 37.78,
            longitude = -122.40,
            lastSpunTimestamp = System.currentTimeMillis() - (6 * 60 * 1000L) // 6 mins ago
        )
        assertFalse("Stop spun 6 mins ago should be ready", expiredStop.isInCooldown())
        assertEquals(0L, expiredStop.remainingCooldownSeconds())
    }
}
