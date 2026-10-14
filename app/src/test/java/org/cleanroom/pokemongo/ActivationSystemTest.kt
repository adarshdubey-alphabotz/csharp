package org.cleanroom.pokemongo

import org.cleanroom.pokemongo.data.activation.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ActivationSystemTest {

    private lateinit var validator: LocalDevActivationValidator

    @Before
    fun setUp() {
        validator = LocalDevActivationValidator()
    }

    @Test
    fun testValidDevelopmentKeys() {
        val now = 1700000000000L

        // 1-Year Dev Key
        val devResult = validator.validateKey("DEV-PRO-2026", now)
        assertTrue("DEV-PRO-2026 should be accepted", devResult.isSuccess)
        val devState = devResult.getOrThrow()
        assertEquals("DEV-PRO-2026", devState.licenseKey)
        assertEquals(ActivationTier.PRO, devState.tier)
        val expectedDevExpiry = now + (365L * 24 * 60 * 60 * 1000L)
        assertEquals(expectedDevExpiry, devState.expiryTimestamp)
        assertFalse("Key should not be expired at start", devState.isExpired(now))
        assertEquals(365L, devState.remainingDays(now))

        // 7-Day Trial Key
        val trialResult = validator.validateKey("TRIAL-7DAYS", now)
        assertTrue("TRIAL-7DAYS should be accepted", trialResult.isSuccess)
        val trialState = trialResult.getOrThrow()
        assertEquals(7L, trialState.remainingDays(now))
        assertFalse(trialState.isExpired(now))

        // Lifetime Key
        val lifetimeResult = validator.validateKey("LIFETIME-PRO", now)
        assertTrue("LIFETIME-PRO should be accepted", lifetimeResult.isSuccess)
        val lifetimeState = lifetimeResult.getOrThrow()
        assertEquals(0L, lifetimeState.expiryTimestamp)
        assertFalse("Lifetime key should never expire", lifetimeState.isExpired(now + 100000000000L))
        assertEquals(9999L, lifetimeState.remainingDays(now))

        // Pattern-matched Key (PRO-XXXX-XXXX)
        val patternResult = validator.validateKey("PRO-ABCD-1234", now)
        assertTrue("PRO-ABCD-1234 should be accepted", patternResult.isSuccess)
        val patternState = patternResult.getOrThrow()
        assertEquals(30L, patternState.remainingDays(now))
    }

    @Test
    fun testInvalidOrMalformedKeys() {
        val now = 1700000000000L

        // Empty key
        val emptyRes = validator.validateKey("", now)
        assertTrue("Empty key must fail", emptyRes.isFailure)

        // Random invalid key
        val invalidRes = validator.validateKey("INVALID-KEY-123", now)
        assertTrue("Invalid key must fail", invalidRes.isFailure)

        // Case insensitivity & trimming
        val trimRes = validator.validateKey("   dev-pro-2026  ", now)
        assertTrue("Trimmed lowercase key should be accepted", trimRes.isSuccess)
    }

    @Test
    fun testLicenseExpirationBehavior() {
        val now = 1700000000000L
        val expiry = now + 1000L // Expires in 1 second
        val state = ActivationState.Active("TEMP-KEY", expiry, ActivationTier.PRO)

        assertFalse("Active before expiry", state.isExpired(now))
        assertTrue("Expired after timestamp passed", state.isExpired(now + 2000L))
        assertEquals(0L, state.remainingDays(now + 2000L))
    }

    @Test
    fun testProFeatureEnumeration() {
        val features = ProFeature.values()
        assertTrue("Must include all target Pro features", features.size >= 10)
        assertTrue(features.contains(ProFeature.AUTO_CATCH))
        assertTrue(features.contains(ProFeature.AUTO_SPIN))
        assertTrue(features.contains(ProFeature.AUTO_FEED))
        assertTrue(features.contains(ProFeature.JOYSTICK))
        assertTrue(features.contains(ProFeature.SPEED_CONTROLS))
        assertTrue(features.contains(ProFeature.DOUBLE_TAP_TELEPORT))
        assertTrue(features.contains(ProFeature.ENHANCED_THROW))
        assertTrue(features.contains(ProFeature.ENCOUNTER_IV))
        assertTrue(features.contains(ProFeature.CAUGHT_PREVIEW))
        assertTrue(features.contains(ProFeature.INVENTORY_IV))
    }
}
