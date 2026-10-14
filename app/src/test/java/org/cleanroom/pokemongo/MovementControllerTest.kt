package org.cleanroom.pokemongo

import org.cleanroom.pokemongo.data.movement.GeoUtils
import org.cleanroom.pokemongo.data.movement.MovementSpeed
import org.cleanroom.pokemongo.data.movement.TeleportCooldownInfo
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class MovementControllerTest {

    @Test
    fun testHaversineDistanceCalculations() {
        // San Francisco Ferry Building to Union Square: approx 2.0 - 2.5 km
        val lat1 = 37.7955
        val lon1 = -122.3937
        val lat2 = 37.7879
        val lon2 = -122.4074

        val distMeters = GeoUtils.distanceMeters(lat1, lon1, lat2, lon2)
        val distKm = GeoUtils.distanceKm(lat1, lon1, lat2, lon2)

        assertTrue("Distance should be approximately 1.5 - 2.5 km", distKm in 1.4..2.5)
        assertEquals(distMeters / 1000.0, distKm, 0.001)

        // Zero distance
        val zeroDist = GeoUtils.distanceMeters(lat1, lon1, lat1, lon1)
        assertEquals(0.0, zeroDist, 0.01)
    }

    @Test
    fun testTeleportCooldownTiers() {
        // Zero distance (<50m) -> 0 cooldown
        assertEquals(0L, GeoUtils.calculateTeleportCooldownSeconds(0.02))

        // Short distances (<1km)
        val cd500m = GeoUtils.calculateTeleportCooldownSeconds(0.5)
        assertEquals("500m cooldown should be 30s", 30L, cd500m)

        // 1 - 5 km tier
        val cd5km = GeoUtils.calculateTeleportCooldownSeconds(5.0)
        assertEquals("5km cooldown should be 240s (4 mins)", 240L, cd5km)

        // 10 km tier
        val cd10km = GeoUtils.calculateTeleportCooldownSeconds(10.0)
        assertEquals("10km cooldown should be 420s (7 mins)", 420L, cd10km)

        // 25 km tier
        val cd25km = GeoUtils.calculateTeleportCooldownSeconds(25.0)
        assertEquals("25km cooldown should be 660s (11 mins)", 660L, cd25km)

        // 50 km tier
        val cd50km = GeoUtils.calculateTeleportCooldownSeconds(50.0)
        assertEquals("50km cooldown should be 1200s (20 mins)", 1200L, cd50km)

        // 100 km tier
        val cd100km = GeoUtils.calculateTeleportCooldownSeconds(100.0)
        assertEquals("100km cooldown should be 2100s (35 mins)", 2100L, cd100km)

        // 250 km tier
        val cd250km = GeoUtils.calculateTeleportCooldownSeconds(250.0)
        assertEquals("250km cooldown should be 2700s (45 mins)", 2700L, cd250km)

        // 500 km tier
        val cd500km = GeoUtils.calculateTeleportCooldownSeconds(500.0)
        assertEquals("500km cooldown should be 3900s (65 mins)", 3900L, cd500km)

        // 1000 km tier
        val cd1000km = GeoUtils.calculateTeleportCooldownSeconds(1000.0)
        assertEquals("1000km cooldown should be 5400s (90 mins)", 5400L, cd1000km)

        // 2000 km tier (Max softban cooldown cap)
        val cd2000km = GeoUtils.calculateTeleportCooldownSeconds(2000.0)
        assertEquals("Long distance cooldown must cap at 7200s (2 hours)", 7200L, cd2000km)
    }

    @Test
    fun testTeleportCooldownStateModel() {
        val now = 1700000000000L
        val cooldownSec = 1200L // 20 mins
        val expiresAt = now + (cooldownSec * 1000L)

        val info = TeleportCooldownInfo(
            distanceKm = 50.0,
            cooldownSeconds = cooldownSec,
            triggeredAtTimestamp = now,
            expiresAtTimestamp = expiresAt
        )

        assertFalse(info.isExpired(now))
        assertEquals(1200L, info.remainingSeconds(now))

        // Halfway through
        val midway = now + 600000L
        assertFalse(info.isExpired(midway))
        assertEquals(600L, info.remainingSeconds(midway))

        // After expiry
        val after = expiresAt + 1000L
        assertTrue(info.isExpired(after))
        assertEquals(0L, info.remainingSeconds(after))
    }

    @Test
    fun testMovementSpeedConversions() {
        // Walk: 9.3 km/h -> approx 2.583 m/s
        assertEquals(9.3f, MovementSpeed.WALK.speedKmh, 0.01f)
        assertEquals(9.3f / 3.6f, MovementSpeed.WALK.speedMps, 0.01f)

        // Run: 18.5 km/h -> approx 5.138 m/s
        assertEquals(18.5f, MovementSpeed.RUN.speedKmh, 0.01f)
        assertEquals(18.5f / 3.6f, MovementSpeed.RUN.speedMps, 0.01f)

        // Cycle: 30.0 km/h -> approx 8.333 m/s
        assertEquals(30.0f, MovementSpeed.CYCLE.speedKmh, 0.01f)
        assertEquals(30.0f / 3.6f, MovementSpeed.CYCLE.speedMps, 0.01f)

        // Drive: 60.0 km/h -> approx 16.666 m/s
        assertEquals(60.0f, MovementSpeed.DRIVE.speedKmh, 0.01f)
        assertEquals(60.0f / 3.6f, MovementSpeed.DRIVE.speedMps, 0.01f)
    }
}
