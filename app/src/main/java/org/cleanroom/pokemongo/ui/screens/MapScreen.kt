package org.cleanroom.pokemongo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.cleanroom.pokemongo.data.activation.ActivationManager
import org.cleanroom.pokemongo.data.activation.ProFeature
import org.cleanroom.pokemongo.data.automation.AutomationEngine
import org.cleanroom.pokemongo.data.models.*
import org.cleanroom.pokemongo.data.movement.*
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.data.repository.PokedexDatabase
import org.cleanroom.pokemongo.data.sniper.SniperFeedType
import org.cleanroom.pokemongo.data.sniper.SniperManager
import org.cleanroom.pokemongo.data.sniper.SniperTarget
import org.cleanroom.pokemongo.ui.theme.*
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun MapScreen(
    repo: GameStateRepository,
    onEncounterPokemon: (WildPokemonSpawn) -> Unit,
    onOpenPokestop: (Pokestop) -> Unit,
    onOpenGym: (Gym) -> Unit,
    onOpenMenu: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenBuddy: () -> Unit
) {
    val context = LocalContext.current
    val activationManager = remember { ActivationManager.getInstance(context) }
    val movementController = remember { MovementController.getInstance(repo) }
    val automationEngine = remember { AutomationEngine.getInstance(context, repo, activationManager) }
    val sniperManager = remember { SniperManager(activationManager) }
    val routeManager = remember { RouteManager(movementController, activationManager) }

    val playerLat by repo.playerLatitude.collectAsState()
    val playerLon by repo.playerLongitude.collectAsState()
    val wildSpawns by repo.wildSpawns.collectAsState()
    val pokestops by repo.pokestops.collectAsState()
    val gyms by repo.gyms.collectAsState()
    val buddy by repo.buddy.collectAsState()
    val trainer by repo.trainerProfile.collectAsState()

    val currentSpeed by movementController.speed.collectAsState()
    val teleportCooldown by movementController.teleportCooldown.collectAsState()
    val isWalkingToWaypoint by movementController.isWalking.collectAsState()
    val tapWaypoint by movementController.tapToWalkDestination.collectAsState()
    val automationConfig by automationEngine.config.collectAsState()
    val patrolState by routeManager.patrolState.collectAsState()

    val isProActive = activationManager.isProActive()

    // Dialog & overlay states
    var showSniperModal by remember { mutableStateOf(false) }
    var showRouteModal by remember { mutableStateOf(false) }
    var showContextDrawer by remember { mutableStateOf(false) }
    var sniperFeedTab by remember { mutableStateOf(SniperFeedType.HUNDO_100IV) }
    var isAppOverlayHidden by remember { mutableStateOf(false) }

    // Pulse animation for player detection circle
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = 110f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    // Tap-to-walk continuous stepping coroutine
    LaunchedEffect(isWalkingToWaypoint) {
        while (isWalkingToWaypoint) {
            delay(100)
            movementController.stepTowardsDestination(0.1f)
        }
    }

    // Route waypoint patrol advance check
    LaunchedEffect(patrolState.isRunning, playerLat, playerLon) {
        if (patrolState.isRunning) {
            routeManager.checkAndAdvanceWaypoint(playerLat, playerLon)
        }
    }

    // Joystick state
    var joystickDragOffset by remember { mutableStateOf(Offset.Zero) }
    val maxJoystickRadius = 110f

    // Joystick continuous movement loop
    LaunchedEffect(joystickDragOffset) {
        val magnitude = sqrt(joystickDragOffset.x * joystickDragOffset.x + joystickDragOffset.y * joystickDragOffset.y)
        if (magnitude > 10f) {
            val vx = (joystickDragOffset.x / maxJoystickRadius).coerceIn(-1.0f, 1.0f)
            val vy = (-joystickDragOffset.y / maxJoystickRadius).coerceIn(-1.0f, 1.0f)
            while (true) {
                movementController.applyJoystickVector(vx, vy, 0.08f)
                delay(80)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Interactive World Map Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF2C6341)) // Stylized park grass green
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val dx = (offset.x - centerX) / size.width * 0.0010
                            val dy = -(offset.y - centerY) / size.height * 0.0010
                            val targetLat = playerLat + dy
                            val targetLon = playerLon + dx
                            movementController.setTapToWalkDestination(targetLat, targetLon)
                        },
                        onDoubleTap = { offset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val dx = (offset.x - centerX) / size.width * 0.0035
                            val dy = -(offset.y - centerY) / size.height * 0.0035
                            val targetLat = playerLat + dy
                            val targetLon = playerLon + dx
                            if (activationManager.isFeatureUnlocked(ProFeature.DOUBLE_TAP_TELEPORT)) {
                                movementController.teleportTo(targetLat, targetLon)
                            } else {
                                movementController.setTapToWalkDestination(targetLat, targetLon)
                            }
                        },
                        onLongPress = {
                            if (automationConfig.hideAppWithGestureEnabled) {
                                isAppOverlayHidden = !isAppOverlayHidden
                            }
                        }
                    )
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Draw roads / paths
            drawLine(
                color = RoadGray.copy(alpha = 0.45f),
                start = Offset(0f, size.height * 0.35f),
                end = Offset(size.width, size.height * 0.65f),
                strokeWidth = 32f
            )
            drawLine(
                color = RoadGray.copy(alpha = 0.45f),
                start = Offset(size.width * 0.2f, 0f),
                end = Offset(size.width * 0.8f, size.height),
                strokeWidth = 24f
            )

            // Water feature
            drawCircle(
                color = WaterBlue.copy(alpha = 0.7f),
                center = Offset(size.width * 0.82f, size.height * 0.22f),
                radius = 70f
            )

            // Radar pulse ring
            drawCircle(
                color = AccentCyan.copy(alpha = pulseAlpha),
                center = center,
                radius = pulseRadius,
                style = Stroke(width = 3f)
            )

            // Interaction radius ring (40m)
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                center = center,
                radius = 80f,
                style = Stroke(width = 2f)
            )

            // Tap-to-Walk trajectory line and destination pin
            tapWaypoint?.let { wp ->
                val dx = ((wp.second - playerLon) / 0.0010 * size.width).toFloat()
                val dy = -((wp.first - playerLat) / 0.0010 * size.height).toFloat()
                val wpOffset = Offset(center.x + dx, center.y + dy)

                // Trajectory line
                drawLine(
                    color = PokemonYellow.copy(alpha = 0.7f),
                    start = center,
                    end = wpOffset,
                    strokeWidth = 4f
                )

                // Destination Marker Pin
                drawCircle(
                    color = PokemonYellow,
                    center = wpOffset,
                    radius = 12f
                )
                drawCircle(
                    color = PokeballRed,
                    center = wpOffset,
                    radius = 6f
                )
            }

            // Player Avatar Circle
            drawCircle(
                color = Color.White,
                center = center,
                radius = 16f
            )
            drawCircle(
                color = PokemonBlue,
                center = center,
                radius = 12f
            )
        }

        // 2. PokéStop Markers Overlay
        pokestops.forEach { stop ->
            val dx = ((stop.longitude - playerLon) / 0.0010 * 380).toFloat()
            val dy = -((stop.latitude - playerLat) / 0.0010 * 380).toFloat()

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (stop.isInCooldown()) Color(0xFF9333EA) else PokemonBlue)
                    .clickable { onOpenPokestop(stop) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = stop.name,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 3. Gym Towers Overlay
        gyms.forEach { gym ->
            val dx = ((gym.longitude - playerLon) / 0.0010 * 380).toFloat()
            val dy = -((gym.latitude - playerLat) / 0.0010 * 380).toFloat()

            val gymColor = when (gym.team) {
                Team.MYSTIC -> MysticBlue
                Team.VALOR -> ValorRed
                Team.INSTINCT -> InstinctYellow
                else -> Color.Gray
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(gymColor)
                    .clickable { onOpenGym(gym) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = gym.name,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        // 4. Wild Pokémon Spawns Overlay
        wildSpawns.forEach { spawn ->
            val dx = ((spawn.longitude - playerLon) / 0.0010 * 380).toFloat()
            val dy = -((spawn.latitude - playerLat) / 0.0010 * 380).toFloat()
            val species = PokedexDatabase.getById(spawn.speciesId)

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(dx.roundToInt(), dy.roundToInt()) }
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                if (spawn.isShiny) Color(0xFFFACC15) else Color(species?.primaryType?.hexColor ?: 0xFFF59E0B),
                                Color(0xFF0F172A)
                            )
                        )
                    )
                    .clickable { onEncounterPokemon(spawn) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (spawn.isShiny) Icons.Default.AutoAwesome else Icons.Default.CatchingPokemon,
                    contentDescription = species?.name ?: "Spawn",
                    tint = if (spawn.isShiny) PokemonYellow else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Stealth unhide button when overlay is hidden
        if (isAppOverlayHidden) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 40.dp, end = 16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { isAppOverlayHidden = false },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Unhide UI",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (!isAppOverlayHidden) {
            // 5. Top Controls Header & Toolbar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 14.dp, end = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pro Badge & Status
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceDark.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isProActive) Color(0xFF10B981) else Color(0xFF64748B))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isProActive) "PRO ACTIVATED" else "FREE",
                            color = if (isProActive) AccentCyan else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Dynamic Custom Shortcuts Toolbar
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SurfaceDark.copy(alpha = 0.85f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (shortcut in automationConfig.customShortcuts) {
                            when (shortcut.uppercase()) {
                                "SNIPER" -> {
                                    IconButton(
                                        onClick = { showSniperModal = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.MyLocation, "Sniper", tint = if (isProActive) PokemonYellow else TextMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                                "ROUTES" -> {
                                    IconButton(
                                        onClick = { showRouteModal = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.AltRoute, "Routes", tint = if (isProActive) AccentCyan else TextMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                                "TOOLS" -> {
                                    IconButton(
                                        onClick = { showContextDrawer = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Tune, "Tools", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                "HEAL" -> {
                                    IconButton(
                                        onClick = { repo.healAllPokemon() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Healing, "Heal All", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                    }
                                }
                                "SPAWNS" -> {
                                    IconButton(
                                        onClick = {
                                            val mult = if (isProActive) automationConfig.spawnBoosterMultiplier else 1
                                            repo.refreshWildSpawns(mult)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, "Refresh Spawns", tint = AccentCyan, modifier = Modifier.size(18.dp))
                                    }
                                }
                                "RESET" -> {
                                    IconButton(
                                        onClick = {
                                            movementController.resetMovementAndUi()
                                            repo.resetUiState()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.RestartAlt, "Reset UI", tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                    }
                                }
                                "STEALTH" -> {
                                    IconButton(
                                        onClick = { isAppOverlayHidden = true },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.VisibilityOff, "Stealth Mode", tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Speed Selector Toolbar
                if (isProActive && automationConfig.speedControlsEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark.copy(alpha = 0.85f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MovementSpeed.values().forEach { speed ->
                        val isSelected = currentSpeed == speed
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PokemonBlue else Color.Transparent)
                                .clickable { movementController.setSpeed(speed) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${speed.speedKmh.toInt()} km/h",
                                color = if (isSelected) Color.White else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Route Patrol Active Banner
            if (patrolState.isRunning && patrolState.activeRoute != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7).copy(alpha = 0.9f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Route Patrol: ${patrolState.activeRoute?.name}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Waypoint ${patrolState.currentWaypointIndex + 1} / ${patrolState.activeRoute?.waypoints?.size ?: 0} (${patrolState.activeRoute?.mode?.displayName})",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp
                            )
                        }
                        Row {
                            IconButton(onClick = { routeManager.pauseRoute() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { routeManager.stopRoute() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Softban Cooldown Banner
            teleportCooldown?.let { cd ->
                if (!cd.isExpired()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.9f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Teleport Softban Cooldown",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val rem = cd.remainingSeconds()
                                val minutes = rem / 60
                                val seconds = rem % 60
                                Text(
                                    text = "Cooldown: ${minutes}m ${seconds}s (Distance: ${String.format("%.1f", cd.distanceKm)} km)",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 10.sp
                                )
                            }
                            IconButton(
                                onClick = { movementController.clearTeleportCooldown() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // 6. Virtual On-Screen Joystick
        if (isProActive && automationConfig.joystickEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 100.dp)
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A).copy(alpha = 0.75f))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val newOffset = joystickDragOffset + dragAmount
                                val dist = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
                                joystickDragOffset = if (dist <= maxJoystickRadius) {
                                    newOffset
                                } else {
                                    newOffset * (maxJoystickRadius / dist)
                                }
                            },
                            onDragEnd = {
                                joystickDragOffset = Offset.Zero
                            },
                            onDragCancel = {
                                joystickDragOffset = Offset.Zero
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Center Knob
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                joystickDragOffset.x.roundToInt(),
                                joystickDragOffset.y.roundToInt()
                            )
                        }
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(AccentCyan, PokemonBlue)
                            )
                        )
                )
            }
        }

        // 7. Bottom Navigation Controls
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            // Main Poké Ball Menu Button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(PokeballRed)
                    .clickable { onOpenMenu() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }

        // Bottom Left: Trainer Profile & Buddy
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Trainer avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(PokemonBlue)
                    .clickable { onOpenProfile() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Lv.${trainer.level}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (automationConfig.hideTrainerName) "Trainer" else trainer.name,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Buddy preview
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF334155))
                    .clickable { onOpenBuddy() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Buddy",
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Bottom Right: Nearby Sightings Radar Panel (when joystick is inactive)
        if (!isProActive || !automationConfig.joystickEnabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark.copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = "Nearby",
                        tint = AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${wildSpawns.size} Nearby",
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

        // 8. Quick Sniper I & II Modal Dialog
        if (showSniperModal) {
            AlertDialog(
                onDismissRequest = { showSniperModal = false },
                title = {
                    Text("Quick Sniper Feed", fontWeight = FontWeight.Bold, color = Color.White)
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = sniperFeedTab == SniperFeedType.HUNDO_100IV,
                                onClick = { sniperFeedTab = SniperFeedType.HUNDO_100IV },
                                label = { Text("Sniper I (100% IV)", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = sniperFeedTab == SniperFeedType.TOP_PVP_RANKS,
                                onClick = { sniperFeedTab = SniperFeedType.TOP_PVP_RANKS },
                                label = { Text("Sniper II (PvP)", fontSize = 11.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val targets = if (sniperFeedTab == SniperFeedType.HUNDO_100IV) {
                            sniperManager.getHundoFeed(playerLat, playerLon, automationConfig.pokemonFeedLanguage)
                        } else {
                            sniperManager.getPvpFeed(playerLat, playerLon, automationConfig.pokemonFeedLanguage)
                        }

                        if (targets.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Sniper requires active Pro Key license.", color = TextMuted, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(targets) { target ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(target.speciesName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("CP ${target.cp} | ${target.cityName}", color = AccentCyan, fontSize = 11.sp)
                                                Text("IV: ${target.ivAttack}/${target.ivDefense}/${target.ivStamina} (${target.ivPercent.toInt()}%)", color = PokemonYellow, fontSize = 10.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    sniperManager.teleportToTarget(target, movementController)
                                                    showSniperModal = false
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = PokemonBlue),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Teleport", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSniperModal = false }) {
                        Text("Close", color = TextMuted)
                    }
                },
                containerColor = SurfaceDark
            )
        }

        // 9. Routes & GPX Walk Modal Dialog
        if (showRouteModal) {
            AlertDialog(
                onDismissRequest = { showRouteModal = false },
                title = {
                    Text("Auto-Walk GPX Routes", fontWeight = FontWeight.Bold, color = Color.White)
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                        Text("Select a pre-configured patrol loop or GPX route:", color = TextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(routeManager.defaultRoutes) { route ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(route.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${route.waypoints.size} Waypoints | ${route.mode.displayName}", color = TextMuted, fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = {
                                                routeManager.startRoute(route)
                                                showRouteModal = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = GrassGreen),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Start", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRouteModal = false }) {
                        Text("Close", color = TextMuted)
                    }
                },
                containerColor = SurfaceDark
            )
        }

        // 10. Quick Context Menu Drawer
        if (showContextDrawer) {
            AlertDialog(
                onDismissRequest = { showContextDrawer = false },
                title = {
                    Text("Companion Quick Tools", fontWeight = FontWeight.Bold, color = Color.White)
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                repo.healAllPokemon()
                                showContextDrawer = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Healing, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Heal & Revive All Pokémon", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                automationEngine.performAutoDiscard()
                                showContextDrawer = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PokeballRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auto-Discard Surplus Items", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                automationEngine.performAutoIncubation()
                                showContextDrawer = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Egg, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Auto-Assign Egg Incubators", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                automationEngine.performGiftAssistant()
                                showContextDrawer = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PokemonBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gift Assistant (Open/Send 10)", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                automationEngine.performQuestAssistant()
                                showContextDrawer = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Quest Assistant (Auto-Snapshots)", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                isAppOverlayHidden = true
                                showContextDrawer = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stealth Mode (Hide All Overlays)", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                movementController.resetMovementAndUi()
                                repo.resetUiState()
                                showContextDrawer = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset UI (Position & Waypoints)", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showContextDrawer = false }) {
                        Text("Close", color = TextMuted)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}
