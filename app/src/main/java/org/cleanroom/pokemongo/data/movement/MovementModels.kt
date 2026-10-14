package org.cleanroom.pokemongo.data.movement

import kotlin.math.*

enum class MovementSpeed(val speedKmh: Float, val displayName: String) {
    WALK(9.3f, "Walk (9.3 km/h)"),
    RUN(18.5f, "Run (18.5 km/h)"),
    CYCLE(30.0f, "Cycle (30 km/h)"),
    DRIVE(60.0f, "Drive (60 km/h)");

    val speedMps: Float
        get() = speedKmh / 3.6f
}

data class TeleportCooldownInfo(
    val distanceKm: Double,
    val cooldownSeconds: Long,
    val triggeredAtTimestamp: Long,
    val expiresAtTimestamp: Long
) {
    fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean {
        return currentTime >= expiresAtTimestamp
    }

    fun remainingSeconds(currentTime: Long = System.currentTimeMillis()): Long {
        return ((expiresAtTimestamp - currentTime) / 1000L).coerceAtLeast(0L)
    }
}

object GeoUtils {
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates distance in meters between two coordinates using Haversine formula
     */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return distanceMeters(lat1, lon1, lat2, lon2) / 1000.0
    }

    /**
     * Calculates softban cooldown based on teleportation distance in kilometers.
     * Follows standard GPS spoofing cooldown guidelines:
     * - Under 1km: 30s to 1m
     * - 5km: 4m
     * - 10km: 7m
     * - 25km: 11m
     * - 50km: 20m
     * - 100km: 35m
     * - 250km: 45m
     * - 500km: 65m
     * - 1000km: 90m
     * - 1500km+: 120m (7200s cap)
     */
    fun calculateTeleportCooldownSeconds(distanceKm: Double): Long {
        if (distanceKm < 0.05) return 0L
        return when {
            distanceKm <= 1.0 -> (distanceKm * 60.0).roundToLong().coerceAtLeast(15L)
            distanceKm <= 5.0 -> (60.0 + (distanceKm - 1.0) * 45.0).roundToLong()
            distanceKm <= 10.0 -> (240.0 + (distanceKm - 5.0) * 36.0).roundToLong()
            distanceKm <= 25.0 -> (420.0 + (distanceKm - 10.0) * 16.0).roundToLong()
            distanceKm <= 50.0 -> (660.0 + (distanceKm - 25.0) * 21.6).roundToLong()
            distanceKm <= 100.0 -> (1200.0 + (distanceKm - 50.0) * 18.0).roundToLong()
            distanceKm <= 250.0 -> (2100.0 + (distanceKm - 100.0) * 4.0).roundToLong()
            distanceKm <= 500.0 -> (2700.0 + (distanceKm - 250.0) * 4.8).roundToLong()
            distanceKm <= 1000.0 -> (3900.0 + (distanceKm - 500.0) * 3.0).roundToLong()
            distanceKm <= 1500.0 -> (5400.0 + (distanceKm - 1000.0) * 3.6).roundToLong().coerceAtMost(7200L)
            else -> 7200L
        }
    }
}
