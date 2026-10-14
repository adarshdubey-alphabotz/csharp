package org.cleanroom.pokemongo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import org.cleanroom.pokemongo.data.movement.MovementController
import org.cleanroom.pokemongo.data.movement.MovementSpeed
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.data.repository.PokedexDatabase
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

    val isProActive = activationManager.isProActive()

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
                start = Offset(size.width * 0.25f, 0f),
                end = Offset(size.width * 0.75f, size.height),
                strokeWidth = 26f
            )

            // Draw water pond feature
            drawCircle(
                color = WaterBlue.copy(alpha = 0.7f),
                center = Offset(size.width * 0.85f, size.height * 0.25f),
                radius = 120f
            )

            // Draw Tap-to-walk Waypoint Destination Marker if set
            tapWaypoint?.let { wp ->
                val wpScreenX = center.x + ((wp.second - playerLon) * 500000.0).toFloat()
                val wpScreenY = center.y - ((wp.first - playerLat) * 500000.0).toFloat()
                drawCircle(
                    color = PokemonYellow.copy(alpha = 0.85f),
                    center = Offset(wpScreenX, wpScreenY),
                    radius = 16f
                )
                drawLine(
                    color = PokemonYellow.copy(alpha = 0.5f),
                    start = center,
                    end = Offset(wpScreenX, wpScreenY),
                    strokeWidth = 3f
                )
            }

            // Draw player radar pulse circle
            drawCircle(
                color = AccentCyan.copy(alpha = pulseAlpha),
                center = center,
                radius = pulseRadius,
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = AccentCyan.copy(alpha = 0.15f),
                center = center,
                radius = 80f
            )

            // Draw Player Marker
            drawCircle(
                color = Color.White,
                center = center,
                radius = 12f
            )
            drawCircle(
                color = PokemonBlue,
                center = center,
                radius = 9f
            )
        }

        // 2. PokéStops Overlays
        pokestops.forEach { stop ->
            val screenX = ((stop.longitude - playerLon) * 500000.0).toFloat()
            val screenY = -((stop.latitude - playerLat) * 500000.0).toFloat()

            Box(
                modifier = Modifier
                    .offset(x = (200 + screenX).dp, y = (320 + screenY).dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (stop.isInCooldown()) Color(0xFF9333EA) else PokemonBlue)
                    .clickable { onOpenPokestop(stop) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = stop.name,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stop.name.take(6),
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 3. Gym Overlays
        gyms.forEach { gym ->
            val screenX = ((gym.longitude - playerLon) * 500000.0).toFloat()
            val screenY = -((gym.latitude - playerLat) * 500000.0).toFloat()

            Box(
                modifier = Modifier
                    .offset(x = (120 + screenX).dp, y = (220 + screenY).dp)
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (gym.team) {
                            Team.MYSTIC -> MysticBlue
                            Team.VALOR -> ValorRed
                            Team.INSTINCT -> InstinctYellow
                            Team.NONE -> Color.Gray
                        }
                    )
                    .clickable { onOpenGym(gym) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = gym.name,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "${gym.defenderCp} CP",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // 4. Wild Pokémon Spawns Overlays
        wildSpawns.forEach { spawn ->
            val species = PokedexDatabase.getById(spawn.speciesId)
            val screenX = ((spawn.longitude - playerLon) * 500000.0).toFloat()
            val screenY = -((spawn.latitude - playerLat) * 500000.0).toFloat()

            Box(
                modifier = Modifier
                    .offset(x = (180 + screenX).dp, y = (290 + screenY).dp)
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(species?.primaryType?.hexColor ?: 0xFF3B82F6).copy(alpha = 0.9f))
                    .clickable { onEncounterPokemon(spawn) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = species?.name ?: "Pokémon",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Lv.${spawn.level.toInt()}",
                        color = PokemonYellow,
                        fontSize = 9.sp
                    )
                }
            }
        }

        // 5. Top Bar: Header, Pro Badge, Refresh, and Cooldown Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
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
                        .padding(horizontal = 12.dp, vertical = 6.dp),
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

                // Top Controls: Directional Arrows & Refresh Spawns
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceDark.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { movementController.applyJoystickVector(0f, 1f, 0.2f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, "North", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { movementController.applyJoystickVector(0f, -1f, 0.2f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, "South", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { movementController.applyJoystickVector(-1f, 0f, 0.2f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, "West", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { movementController.applyJoystickVector(1f, 0f, 0.2f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowForward, "East", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { repo.refreshWildSpawns() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Refresh, "Refresh Spawns", tint = AccentCyan, modifier = Modifier.size(18.dp))
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
            // Main Pokéball Menu Button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(PokeballRed, PokeballDarkRed, Color.White, Color.White)
                        )
                    )
                    .clickable { onOpenMenu() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    )
                }
            }
        }

        // Bottom Left: Trainer Avatar & Buddy Icon
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Trainer Level Icon
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(trainer.team.let {
                        when (it) {
                            Team.MYSTIC -> MysticBlue
                            Team.VALOR -> ValorRed
                            Team.INSTINCT -> InstinctYellow
                            else -> PokemonBlue
                        }
                    })
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
                        text = trainer.name,
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
}
