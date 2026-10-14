package org.cleanroom.pokemongo.data.movement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cleanroom.pokemongo.data.activation.ActivationManager
import org.cleanroom.pokemongo.data.activation.ProFeature
import java.util.UUID

data class RouteWaypoint(
    val latitude: Double,
    val longitude: Double,
    val label: String
)

enum class RouteLoopMode(val displayName: String) {
    LOOP("Circular Loop"),
    PING_PONG("Ping-Pong (Reverse on End)"),
    ONCE("Single Pass")
}

data class Route(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val waypoints: List<RouteWaypoint>,
    val mode: RouteLoopMode = RouteLoopMode.LOOP
)

data class RoutePatrolState(
    val activeRoute: Route? = null,
    val isRunning: Boolean = false,
    val currentWaypointIndex: Int = 0,
    val isReversing: Boolean = false,
    val totalDistanceKm: Float = 0f
)

class RouteManager(
    private val movementController: MovementController,
    private val activationManager: ActivationManager
) {
    private val _patrolState = MutableStateFlow(RoutePatrolState())
    val patrolState: StateFlow<RoutePatrolState> = _patrolState.asStateFlow()

    // Pre-configured GPX hotspot routes
    val defaultRoutes: List<Route> = listOf(
        Route(
            name = "Zaragoza Plaza Hotspot Loop",
            waypoints = listOf(
                RouteWaypoint(41.6561, -0.8773, "Plaza del Pilar"),
                RouteWaypoint(41.6575, -0.8790, "Cathedral North"),
                RouteWaypoint(41.6582, -0.8760, "Ebro River Walk"),
                RouteWaypoint(41.6568, -0.8745, "Puente de Piedra"),
                RouteWaypoint(41.6550, -0.8755, "Plaza South")
            ),
            mode = RouteLoopMode.LOOP
        ),
        Route(
            name = "San Francisco Pier 39 Circuit",
            waypoints = listOf(
                RouteWaypoint(37.8087, -122.4098, "Pier 39 Entrance"),
                RouteWaypoint(37.8100, -122.4105, "Sea Lion Viewing Deck"),
                RouteWaypoint(37.8095, -122.4080, "East Marina"),
                RouteWaypoint(37.8075, -122.4085, "Fisherman's Wharf Central")
            ),
            mode = RouteLoopMode.LOOP
        ),
        Route(
            name = "New York Central Park South Path",
            waypoints = listOf(
                RouteWaypoint(40.7670, -73.9740, "Grand Army Plaza"),
                RouteWaypoint(40.7710, -73.9745, "The Zoo"),
                RouteWaypoint(40.7740, -73.9710, "The Mall & Literary Walk"),
                RouteWaypoint(40.7760, -73.9725, "Bethesda Terrace"),
                RouteWaypoint(40.7810, -73.9665, "The Lake Overlook")
            ),
            mode = RouteLoopMode.PING_PONG
        )
    )

    fun startRoute(route: Route): Boolean {
        if (!activationManager.isFeatureUnlocked(ProFeature.ROUTE_WALK)) {
            return false
        }
        if (route.waypoints.isEmpty()) return false

        _patrolState.value = RoutePatrolState(
            activeRoute = route,
            isRunning = true,
            currentWaypointIndex = 0,
            isReversing = false
        )
        val firstWp = route.waypoints.first()
        movementController.setTapToWalkDestination(firstWp.latitude, firstWp.longitude)
        return true
    }

    fun pauseRoute() {
        _patrolState.value = _patrolState.value.copy(isRunning = false)
        movementController.clearDestination()
    }

    fun resumeRoute(): Boolean {
        if (!activationManager.isFeatureUnlocked(ProFeature.ROUTE_WALK)) return false
        val state = _patrolState.value
        val route = state.activeRoute ?: return false
        if (route.waypoints.isEmpty()) return false

        _patrolState.value = state.copy(isRunning = true)
        val target = route.waypoints[state.currentWaypointIndex.coerceIn(0, route.waypoints.size - 1)]
        movementController.setTapToWalkDestination(target.latitude, target.longitude)
        return true
    }

    fun stopRoute() {
        _patrolState.value = RoutePatrolState()
        movementController.clearDestination()
    }

    /**
     * Advances route progression when player reaches the active waypoint (within 5 meters).
     */
    fun checkAndAdvanceWaypoint(playerLat: Double, playerLon: Double): Boolean {
        val state = _patrolState.value
        if (!state.isRunning || state.activeRoute == null) return false

        val waypoints = state.activeRoute.waypoints
        val currIdx = state.currentWaypointIndex
        if (currIdx !in waypoints.indices) return false

        val targetWp = waypoints[currIdx]
        val dist = GeoUtils.distanceMeters(playerLat, playerLon, targetWp.latitude, targetWp.longitude)

        if (dist <= 5.0) {
            // Reached waypoint! Advance according to LoopMode
            when (state.activeRoute.mode) {
                RouteLoopMode.LOOP -> {
                    val nextIdx = (currIdx + 1) % waypoints.size
                    _patrolState.value = state.copy(currentWaypointIndex = nextIdx)
                    val nextWp = waypoints[nextIdx]
                    movementController.setTapToWalkDestination(nextWp.latitude, nextWp.longitude)
                }
                RouteLoopMode.PING_PONG -> {
                    var isRev = state.isReversing
                    var nextIdx = if (isRev) currIdx - 1 else currIdx + 1
                    if (nextIdx >= waypoints.size) {
                        isRev = true
                        nextIdx = waypoints.size - 2
                    } else if (nextIdx < 0) {
                        isRev = false
                        nextIdx = 1
                    }
                    _patrolState.value = state.copy(currentWaypointIndex = nextIdx, isReversing = isRev)
                    val nextWp = waypoints[nextIdx]
                    movementController.setTapToWalkDestination(nextWp.latitude, nextWp.longitude)
                }
                RouteLoopMode.ONCE -> {
                    val nextIdx = currIdx + 1
                    if (nextIdx < waypoints.size) {
                        _patrolState.value = state.copy(currentWaypointIndex = nextIdx)
                        val nextWp = waypoints[nextIdx]
                        movementController.setTapToWalkDestination(nextWp.latitude, nextWp.longitude)
                    } else {
                        stopRoute()
                    }
                }
            }
            return true
        }
        return false
    }

    fun parseGpxCoordinates(name: String, gpxString: String): Route {
        val regex = Regex("""lat=["']([-0-9.]+)["']\s+lon=["']([-0-9.]+)["']""")
        val waypoints = mutableListOf<RouteWaypoint>()
        var count = 1
        for (match in regex.findAll(gpxString)) {
            val lat = match.groupValues[1].toDoubleOrNull()
            val lon = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lon != null) {
                waypoints.add(RouteWaypoint(lat, lon, "Point $count"))
                count++
            }
        }
        return Route(name = name, waypoints = waypoints)
    }
}
