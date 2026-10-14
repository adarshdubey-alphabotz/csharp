package org.cleanroom.pokemongo.data.movement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import kotlin.math.*

class MovementController(
    private val repo: GameStateRepository
) {
    private val _speed = MutableStateFlow(MovementSpeed.WALK)
    val speed: StateFlow<MovementSpeed> = _speed.asStateFlow()

    private val _teleportCooldown = MutableStateFlow<TeleportCooldownInfo?>(null)
    val teleportCooldown: StateFlow<TeleportCooldownInfo?> = _teleportCooldown.asStateFlow()

    private val _tapToWalkDestination = MutableStateFlow<Pair<Double, Double>?>(null)
    val tapToWalkDestination: StateFlow<Pair<Double, Double>?> = _tapToWalkDestination.asStateFlow()

    private val _isWalking = MutableStateFlow(false)
    val isWalking: StateFlow<Boolean> = _isWalking.asStateFlow()

    fun setSpeed(newSpeed: MovementSpeed) {
        _speed.value = newSpeed
    }

    /**
     * Instantly relocates player to destination coordinates and computes softban cooldown.
     */
    fun teleportTo(destLat: Double, destLon: Double, currentTime: Long = System.currentTimeMillis()): TeleportCooldownInfo {
        val currentLat = repo.playerLatitude.value
        val currentLon = repo.playerLongitude.value
        val distanceKm = GeoUtils.distanceKm(currentLat, currentLon, destLat, destLon)
        val cooldownSec = GeoUtils.calculateTeleportCooldownSeconds(distanceKm)
        val expiresAt = currentTime + (cooldownSec * 1000L)

        val cooldownInfo = TeleportCooldownInfo(
            distanceKm = distanceKm,
            cooldownSeconds = cooldownSec,
            triggeredAtTimestamp = currentTime,
            expiresAtTimestamp = expiresAt
        )
        _teleportCooldown.value = cooldownInfo
        _tapToWalkDestination.value = null
        _isWalking.value = false

        // Directly move player location without accumulating thousands of km in egg incubation
        // (teleport is instant spatial shift, not walking)
        repo.updatePlayerLocationDirect(destLat, destLon)
        return cooldownInfo
    }

    /**
     * Applies directional joystick displacement vector (vx, vy in range [-1.0, 1.0]).
     * Coordinates are updated and walking distance is accumulated.
     */
    fun applyJoystickVector(vx: Float, vy: Float, deltaTimeSeconds: Float = 0.1f) {
        val magnitude = sqrt(vx * vx + vy * vy).coerceIn(0.0f, 1.0f)
        if (magnitude < 0.05f) return

        val speedMps = _speed.value.speedMps
        val distanceMeters = speedMps * deltaTimeSeconds * magnitude

        val currentLat = repo.playerLatitude.value
        val currentLon = repo.playerLongitude.value

        // Convert displacement to lat/lon degrees
        // 1 deg lat ~ 111,320m
        // 1 deg lon ~ 111,320m * cos(lat)
        val latRad = Math.toRadians(currentLat)
        val metersPerDegLat = 111320.0
        val metersPerDegLon = 111320.0 * cos(latRad).coerceAtLeast(0.01)

        val normVx = vx / magnitude
        val normVy = vy / magnitude

        val dLon = (normVx * distanceMeters) / metersPerDegLon
        val dLat = (normVy * distanceMeters) / metersPerDegLat

        repo.updatePlayerLocation(currentLat + dLat, currentLon + dLon)
    }

    /**
     * Sets waypoint for tap-to-walk navigation.
     */
    fun setTapToWalkDestination(destLat: Double, destLon: Double) {
        _tapToWalkDestination.value = Pair(destLat, destLon)
        _isWalking.value = true
    }

    /**
     * Advances player one step towards tap-to-walk waypoint.
     * Returns true when destination is reached (within 2 meters).
     */
    fun stepTowardsDestination(deltaTimeSeconds: Float = 0.1f): Boolean {
        val dest = _tapToWalkDestination.value ?: return true
        val currentLat = repo.playerLatitude.value
        val currentLon = repo.playerLongitude.value

        val distanceRemaining = GeoUtils.distanceMeters(currentLat, currentLon, dest.first, dest.second)
        if (distanceRemaining <= 2.0) {
            _tapToWalkDestination.value = null
            _isWalking.value = false
            return true
        }

        val stepMeters = (_speed.value.speedMps * deltaTimeSeconds).toDouble()
        val fraction = (stepMeters / distanceRemaining).coerceAtMost(1.0)

        val newLat = currentLat + (dest.first - currentLat) * fraction
        val newLon = currentLon + (dest.second - currentLon) * fraction

        repo.updatePlayerLocation(newLat, newLon)

        if (fraction >= 1.0) {
            _tapToWalkDestination.value = null
            _isWalking.value = false
            return true
        }
        return false
    }

    fun cancelTapToWalk() {
        _tapToWalkDestination.value = null
        _isWalking.value = false
    }

    fun clearTeleportCooldown() {
        _teleportCooldown.value = null
    }

    companion object {
        @Volatile
        private var instance: MovementController? = null

        fun getInstance(repo: GameStateRepository): MovementController {
            return instance ?: synchronized(this) {
                instance ?: MovementController(repo).also { instance = it }
            }
        }
    }
}
