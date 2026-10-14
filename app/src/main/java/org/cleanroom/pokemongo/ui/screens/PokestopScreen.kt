package org.cleanroom.pokemongo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cleanroom.pokemongo.data.models.Pokestop
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.data.repository.SpinResult
import org.cleanroom.pokemongo.ui.theme.*

@Composable
fun PokestopScreen(
    pokestop: Pokestop,
    repo: GameStateRepository,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isSpinning by remember { mutableStateOf(false) }
    var spinResult by remember { mutableStateOf<SpinResult?>(null) }
    val isCooldown = pokestop.isInCooldown()

    val rotation = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close Button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.Start)
                    .clip(CircleShape)
                    .background(SurfaceDark)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // PokéStop Title
            Text(
                text = pokestop.name,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isCooldown) "In cooldown (${pokestop.remainingCooldownSeconds()}s left)" else "Swipe photo disc to spin!",
                color = if (isCooldown) Color(0xFFC084FC) else AccentCyan,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            // Interactive Photo Disc
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                if (isCooldown) Color(0xFF7E22CE) else PokemonBlue,
                                if (isCooldown) Color(0xFF3B0764) else PokeDarkBlue
                            )
                        )
                    )
                    .rotate(rotation.value)
                    .pointerInput(isCooldown) {
                        if (!isCooldown) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                if (dragAmount > 20 && !isSpinning) {
                                    isSpinning = true
                                    coroutineScope.launch {
                                        rotation.animateTo(
                                            targetValue = rotation.value + 1080f,
                                            animationSpec = tween(1200, easing = FastOutSlowInEasing)
                                        )
                                        val res = repo.spinPokestop(pokestop.id)
                                        spinResult = res
                                        isSpinning = false
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        center = center,
                        radius = r - 10f,
                        style = Stroke(width = 8f)
                    )
                    // Photo disc center landmark icon
                    drawCircle(
                        color = if (isCooldown) Color(0xFF9333EA) else AccentCyan,
                        center = center,
                        radius = r - 35f
                    )
                }

                Text(
                    text = if (isCooldown) "COOLDOWN" else "SPIN",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Loot Items Result Display
            if (spinResult != null) {
                val res = spinResult!!
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "+${res.xpAwarded} XP",
                            color = AccentCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            res.itemsAwarded.forEach { (item, count) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = when (item.id) {
                                            "poke_ball" -> "⚪"
                                            "potion" -> "🧪"
                                            else -> "🍓"
                                        },
                                        fontSize = 24.sp
                                    )
                                    Text(
                                        text = "+$count ${item.displayName}",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (res.eggAwarded != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "🥚", fontSize = 24.sp)
                                    Text(
                                        text = "${res.eggAwarded.distanceKm.toInt()}km Egg!",
                                        color = PokemonYellow,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
