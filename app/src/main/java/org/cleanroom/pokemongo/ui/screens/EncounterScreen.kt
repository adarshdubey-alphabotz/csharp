package org.cleanroom.pokemongo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cleanroom.pokemongo.data.activation.ActivationManager
import org.cleanroom.pokemongo.data.activation.ProFeature
import org.cleanroom.pokemongo.data.automation.AutomationEngine
import org.cleanroom.pokemongo.data.models.ItemType
import org.cleanroom.pokemongo.data.models.WildPokemonSpawn
import org.cleanroom.pokemongo.data.repository.*
import org.cleanroom.pokemongo.ui.theme.*

@Composable
fun EncounterScreen(
    spawn: WildPokemonSpawn,
    repo: GameStateRepository,
    onCatchComplete: () -> Unit,
    onRunAway: () -> Unit
) {
    val context = LocalContext.current
    val activationManager = remember { ActivationManager.getInstance(context) }
    val automationEngine = remember { AutomationEngine.getInstance(context, repo, activationManager) }

    val species = remember { PokedexDatabase.getById(spawn.speciesId) ?: PokedexDatabase.speciesList.first() }
    val items by repo.items.collectAsState()
    val automationConfig by automationEngine.config.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val isProActive = activationManager.isProActive()
    val isEncounterIvUnlocked = activationManager.isFeatureUnlocked(ProFeature.ENCOUNTER_IV) && automationConfig.encounterIvEnabled
    val ivDetails = remember(spawn) { automationEngine.calculateEncounterIv(spawn) }

    var selectedBall by remember { mutableStateOf(automationConfig.preferredBall.takeIf { (items[it] ?: 0) > 0 } ?: ItemType.POKE_BALL) }
    var activeBerry by remember { mutableStateOf<ItemType?>(null) }
    var showBerryDrawer by remember { mutableStateOf(false) }
    var showBallDrawer by remember { mutableStateOf(false) }

    // Throwing state
    var isThrowing by remember { mutableStateOf(false) }
    var ballPosition by remember { mutableStateOf(Offset(0f, 0f)) }
    var encounterStatusText by remember { mutableStateOf<String?>(null) }
    var caughtPreviewBanner by remember { mutableStateOf<String?>(null) }
    var isShakingBall by remember { mutableStateOf(false) }
    var shakeCount by remember { mutableStateOf(0) }
    var catchSuccessResult by remember { mutableStateOf<CatchResult.Success?>(null) }
    var hasFled by remember { mutableStateOf(false) }

    // Shrinking Target Ring Animation
    val ringTransition = rememberInfiniteTransition(label = "ring")
    val ringRadius by ringTransition.animateFloat(
        initialValue = 90f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRadius"
    )

    // Ball Shake Animation
    val shakeTransition = rememberInfiniteTransition(label = "shake")
    val shakeAngle by shakeTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shakeAngle"
    )

    val ringColor = remember(species.baseCaptureRate, selectedBall, activeBerry) {
        val base = species.baseCaptureRate * selectedBall.catchMultiplier * (activeBerry?.catchMultiplier ?: 1.0f)
        when {
            base >= 0.40f -> Color(0xFF22C55E) // Green (Easy)
            base >= 0.25f -> Color(0xFFFACC15) // Yellow (Medium)
            base >= 0.12f -> Color(0xFFF97316) // Orange (Hard)
            else -> Color(0xFFEF4444)          // Red (Very Hard)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Encounter 3D Landscape Background Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Sky gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF60A5FA), Color(0xFF93C5FD), Color(0xFFBFDBFE)),
                    startY = 0f,
                    endY = h * 0.55f
                ),
                topLeft = Offset.Zero,
                size = androidx.compose.ui.geometry.Size(w, h * 0.55f)
            )

            // Meadow grass gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4ADE80), Color(0xFF22C55E), Color(0xFF15803D)),
                    startY = h * 0.55f,
                    endY = h
                ),
                topLeft = Offset(0f, h * 0.55f),
                size = androidx.compose.ui.geometry.Size(w, h * 0.45f)
            )

            // Target ring on wild Pokémon
            val pokemonCenter = Offset(w / 2f, h * 0.45f)

            if (!isShakingBall && catchSuccessResult == null && !hasFled) {
                // White outer ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    center = pokemonCenter,
                    radius = 90f,
                    style = Stroke(width = 4f)
                )

                // Shrinking colored difficulty ring
                drawCircle(
                    color = ringColor,
                    center = pokemonCenter,
                    radius = ringRadius,
                    style = Stroke(width = 6f)
                )

                // Wild Pokémon Avatar Base
                drawCircle(
                    color = Color(species.primaryType.hexColor),
                    center = pokemonCenter,
                    radius = 65f
                )
            }

            // Draw Pokeball when thrown or shaking
            if (isShakingBall) {
                drawCircle(
                    color = PokeballRed,
                    center = pokemonCenter,
                    radius = 28f
                )
                drawCircle(
                    color = Color.White,
                    center = Offset(pokemonCenter.x, pokemonCenter.y + 10f),
                    radius = 16f
                )
            } else if (isThrowing) {
                drawCircle(
                    color = when (selectedBall) {
                        ItemType.GREAT_BALL -> PokemonBlue
                        ItemType.ULTRA_BALL -> Color(0xFFF59E0B)
                        else -> PokeballRed
                    },
                    center = ballPosition,
                    radius = 26f
                )
            }
        }

        // 2. Top Bar: Run Away, Species Name, and Encounter IV HUD
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
                IconButton(
                    onClick = onRunAway,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceDark.copy(alpha = 0.85f))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Run Away", tint = Color.White)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark.copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = species.name,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEncounterIvUnlocked) "CP ${ivDetails.exactCp} (Lv.${spawn.level.toInt()})" else "CP ??? (Lv.${spawn.level.toInt()})",
                        color = AccentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Berry indicator if fed
                if (activeBerry != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFE11D48))
                            .padding(8.dp)
                    ) {
                        Text(text = "Berry", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }

            // Encounter IV HUD Card
            if (isEncounterIvUnlocked) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.90f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // IV Percentage Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        ivDetails.ivPercent >= 100.0f -> Color(0xFFF59E0B) // Gold 100%
                                        ivDetails.ivPercent >= 82.2f -> Color(0xFF10B981) // Green
                                        ivDetails.ivPercent >= 66.7f -> Color(0xFF3B82F6) // Blue
                                        else -> Color(0xFF64748B)
                                    }
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "IV: ${String.format("%.1f%%", ivDetails.ivPercent)} (${ivDetails.stars}★)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Stat Breakdown
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "ATK ${ivDetails.ivAttack}/15",
                                color = if (ivDetails.ivAttack == 15) PokemonYellow else TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "DEF ${ivDetails.ivDefense}/15",
                                color = if (ivDetails.ivDefense == 15) PokemonYellow else TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "STA ${ivDetails.ivStamina}/15",
                                color = if (ivDetails.ivStamina == 15) PokemonYellow else TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Caught Preview Prediction Banner
            caughtPreviewBanner?.let { bannerText ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B).copy(alpha = 0.95f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = bannerText,
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Middle Status Banner (Nice!, Great!, Excellent!, Gotcha!)
        if (encounterStatusText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-110).dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(PokemonYellow)
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(
                    text = encounterStatusText ?: "",
                    color = PokeDarkBlue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // 4. Success Catch Dialog Modal
        if (catchSuccessResult != null) {
            val res = catchSuccessResult!!
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GOTCHA!",
                        color = PokemonYellow,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${species.name} was caught!",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "+${res.xpEarned} XP", color = AccentCyan, fontWeight = FontWeight.Bold)
                            Text(text = "XP Earned", color = TextMuted, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "+${res.stardustEarned}", color = Color(0xFFA855F7), fontWeight = FontWeight.Bold)
                            Text(text = "Stardust", color = TextMuted, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "+${res.candyEarned}", color = PokemonYellow, fontWeight = FontWeight.Bold)
                            Text(text = "Candy", color = TextMuted, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onCatchComplete,
                        colors = ButtonDefaults.buttonColors(containerColor = PokemonBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // 5. Bottom Throw Area & Ball Dragging
        if (!isShakingBall && catchSuccessResult == null && !hasFled) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                when (selectedBall) {
                                    ItemType.GREAT_BALL -> PokemonBlue
                                    ItemType.ULTRA_BALL -> Color(0xFFF59E0B)
                                    else -> PokeballRed
                                },
                                Color.White
                            )
                        )
                    )
                    .pointerInput(selectedBall, activeBerry) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isThrowing = true
                                ballPosition = offset
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                ballPosition = Offset(
                                    ballPosition.x + dragAmount.x,
                                    ballPosition.y + dragAmount.y
                                )
                            },
                            onDragEnd = {
                                // Evaluate throw quality based on ringRadius
                                val baseQuality = when {
                                    ringRadius < 35f -> ThrowQuality.EXCELLENT
                                    ringRadius < 60f -> ThrowQuality.GREAT
                                    else -> ThrowQuality.NICE
                                }

                                // Check Enhanced Throw override
                                val (quality, isCurve) = automationEngine.resolveThrow(baseQuality, true)
                                val isEnhanced = quality == ThrowQuality.EXCELLENT && baseQuality != ThrowQuality.EXCELLENT
                                encounterStatusText = if (isEnhanced) "EXCELLENT! (Enhanced)" else quality.displayName

                                // Caught Preview Prediction
                                val isCaughtPreviewUnlocked = activationManager.isFeatureUnlocked(ProFeature.CAUGHT_PREVIEW) && automationConfig.caughtPreviewEnabled
                                if (isCaughtPreviewUnlocked) {
                                    val prediction = automationEngine.predictCatchOutcome(
                                        spawn = spawn,
                                        ballType = selectedBall,
                                        berry = activeBerry,
                                        quality = quality,
                                        isCurveball = isCurve
                                    )
                                    caughtPreviewBanner = if (prediction.isLikelyCaught) {
                                        "✨ Preview: CAUGHT (${String.format("%.1f%%", prediction.probability * 100)})"
                                    } else {
                                        "⚠️ Preview: BREAKOUT RISK (${String.format("%.1f%%", prediction.probability * 100)})"
                                    }
                                }

                                coroutineScope.launch {
                                    delay(600)
                                    isThrowing = false
                                    isShakingBall = true
                                    encounterStatusText = null

                                    // Simulate 3 shakes
                                    delay(900)
                                    shakeCount = 1
                                    delay(900)
                                    shakeCount = 2
                                    delay(900)
                                    shakeCount = 3

                                    val catchResult = repo.catchPokemon(
                                        spawn = spawn,
                                        ballType = selectedBall,
                                        berry = activeBerry,
                                        throwQuality = quality,
                                        isCurveball = isCurve
                                    )

                                    caughtPreviewBanner = null

                                    when (catchResult) {
                                        is CatchResult.Success -> {
                                            isShakingBall = false
                                            catchSuccessResult = catchResult
                                        }
                                        is CatchResult.Breakout -> {
                                            isShakingBall = false
                                            encounterStatusText = "Break out!"
                                            delay(1000)
                                            encounterStatusText = null
                                            activeBerry = null // Berry consumed on breakout
                                        }
                                        is CatchResult.Fled -> {
                                            isShakingBall = false
                                            hasFled = true
                                            encounterStatusText = "${species.name} fled!"
                                            delay(1500)
                                            onRunAway()
                                        }
                                    }
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${items[selectedBall] ?: 0}",
                    color = PokeDarkBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // Bottom Left: Berry Button
            IconButton(
                onClick = { showBerryDrawer = !showBerryDrawer },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 44.dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark.copy(alpha = 0.9f))
            ) {
                Text("🍇", fontSize = 24.sp)
            }

            // Bottom Right: Ball Selection Button
            IconButton(
                onClick = { showBallDrawer = !showBallDrawer },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 44.dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark.copy(alpha = 0.9f))
            ) {
                Text("⚪", fontSize = 24.sp)
            }
        }

        // Berry Selection Drawer
        if (showBerryDrawer) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceDark.copy(alpha = 0.95f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf(ItemType.RAZZ_BERRY, ItemType.NANAB_BERRY, ItemType.PINAP_BERRY).forEach { berry ->
                    val count = items[berry] ?: 0
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            if (count > 0) {
                                activeBerry = berry
                                showBerryDrawer = false
                            }
                        }
                    ) {
                        Text(
                            text = when (berry) {
                                ItemType.RAZZ_BERRY -> "🍓"
                                ItemType.NANAB_BERRY -> "🍌"
                                else -> "🍍"
                            },
                            fontSize = 28.sp
                        )
                        Text(text = berry.displayName.take(5), color = Color.White, fontSize = 11.sp)
                        Text(text = "x$count", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Ball Selection Drawer
        if (showBallDrawer) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceDark.copy(alpha = 0.95f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf(ItemType.POKE_BALL, ItemType.GREAT_BALL, ItemType.ULTRA_BALL).forEach { ball ->
                    val count = items[ball] ?: 0
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            if (count > 0) {
                                selectedBall = ball
                                showBallDrawer = false
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when (ball) {
                                        ItemType.GREAT_BALL -> PokemonBlue
                                        ItemType.ULTRA_BALL -> Color(0xFFF59E0B)
                                        else -> PokeballRed
                                    }
                                )
                        )
                        Text(text = ball.displayName.take(5), color = Color.White, fontSize = 11.sp)
                        Text(text = "x$count", color = AccentCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
