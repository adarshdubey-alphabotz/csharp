package org.cleanroom.pokemongo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cleanroom.pokemongo.data.activation.ActivationManager
import org.cleanroom.pokemongo.data.activation.ProFeature
import org.cleanroom.pokemongo.data.automation.AutomationEngine
import org.cleanroom.pokemongo.data.models.Gym
import org.cleanroom.pokemongo.data.models.Team
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.ui.theme.*

@Composable
fun GymScreen(
    gym: Gym,
    repo: GameStateRepository,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activationManager = remember { ActivationManager.getInstance(context) }
    val automationEngine = remember { AutomationEngine.getInstance(context, repo, activationManager) }
    val automationConfig by automationEngine.config.collectAsState()
    val isProActive = activationManager.isProActive()

    val coroutineScope = rememberCoroutineScope()
    var isBattling by remember { mutableStateOf(false) }
    var defenderHp by remember { mutableStateOf(gym.defenderCp) }
    var playerHp by remember { mutableStateOf(2400) }
    var energyCharge by remember { mutableStateOf(0) }
    var battleWon by remember { mutableStateOf(false) }

    // Pro Battle Assist States
    var isAutoBattleActive by remember { mutableStateOf(automationConfig.gymBattleAssistEnabled && isProActive) }
    var isDynamaxActive by remember { mutableStateOf(false) }

    val teamColor = when (gym.team) {
        Team.MYSTIC -> MysticBlue
        Team.VALOR -> ValorRed
        Team.INSTINCT -> InstinctYellow
        else -> Color.Gray
    }

    // Automated Battle Loop when Battle Assistant is active
    LaunchedEffect(isBattling, isAutoBattleActive, isDynamaxActive) {
        if (isBattling && isAutoBattleActive) {
            while (isBattling && defenderHp > 0) {
                delay(350)
                val baseDmg = if (isDynamaxActive) 450 else 180
                defenderHp = (defenderHp - baseDmg).coerceAtLeast(0)
                energyCharge = (energyCharge + (if (isDynamaxActive) 35 else 20)).coerceAtMost(100)

                if (energyCharge >= 100) {
                    delay(250)
                    val chargedDmg = if (isDynamaxActive) 2200 else 950
                    defenderHp = (defenderHp - chargedDmg).coerceAtLeast(0)
                    energyCharge = 0
                }

                if (defenderHp <= 0) {
                    battleWon = true
                    isBattling = false
                    repo.addRewards(1000L, 50)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Close Button & Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceDark)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(teamColor)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = gym.team.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = gym.name,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Defender Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CP ${gym.defenderCp}",
                        color = AccentCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = gym.defenderSpeciesName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Defender HP Bar
                    LinearProgressIndicator(
                        progress = { (defenderHp.toFloat() / gym.defenderCp).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (defenderHp > gym.defenderCp * 0.3f) Color(0xFF22C55E) else Color(0xFFEF4444),
                        trackColor = Color(0xFF334155)
                    )
                    Text(
                        text = "HP $defenderHp / ${gym.defenderCp}",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (battleWon) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PokemonBlue)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "VICTORY!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = PokemonYellow
                        )
                        Text(
                            text = "Gym cleared! +1000 XP & +50 PokéCoins",
                            fontSize = 14.sp,
                            color = Color.White,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            } else if (!isBattling) {
                Button(
                    onClick = {
                        isBattling = true
                        defenderHp = gym.defenderCp
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PokeballRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("START BATTLE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                // Active Combat Arena with Auto-Battle & Dynamax Controls
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Assistant Toggles Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Auto-Battle Bot Toggle
                        FilterChip(
                            selected = isAutoBattleActive,
                            onClick = {
                                if (isProActive) {
                                    isAutoBattleActive = !isAutoBattleActive
                                }
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isAutoBattleActive) "Auto-Bot: ON" else "Auto-Bot: OFF", fontSize = 11.sp)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PokemonBlue,
                                selectedLabelColor = Color.White
                            )
                        )

                        // Dynamax Assist Toggle
                        FilterChip(
                            selected = isDynamaxActive,
                            onClick = {
                                if (isProActive) {
                                    isDynamaxActive = !isDynamaxActive
                                }
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isDynamaxActive) "Dynamax: ACTIVE" else "Dynamax", fontSize = 11.sp)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF9333EA),
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isAutoBattleActive) "🤖 Auto-Battle Bot is attacking..." else "Tap quickly to Fast Attack!",
                        color = PokemonYellow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Fast Attack Tap Target
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(if (isDynamaxActive) Color(0xFF9333EA) else PokemonBlue)
                            .clickable {
                                val dmg = if (isDynamaxActive) 450 else 180
                                defenderHp = (defenderHp - dmg).coerceAtLeast(0)
                                energyCharge = (energyCharge + 15).coerceAtMost(100)
                                if (defenderHp <= 0) {
                                    battleWon = true
                                    isBattling = false
                                    repo.addRewards(1000L, 50)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isDynamaxActive) "MAX ATTACK!" else "ATTACK!",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Charged Attack Button
                    Button(
                        onClick = {
                            if (energyCharge >= 100) {
                                val dmg = if (isDynamaxActive) 2200 else 950
                                defenderHp = (defenderHp - dmg).coerceAtLeast(0)
                                energyCharge = 0
                                if (defenderHp <= 0) {
                                    battleWon = true
                                    isBattling = false
                                    repo.addRewards(1000L, 50)
                                }
                            }
                        },
                        enabled = energyCharge >= 100,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDynamaxActive) Color(0xFFE11D48) else PokemonYellow,
                            disabledContainerColor = Color(0xFF475569)
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = "Charged Move", tint = PokeDarkBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (energyCharge >= 100) {
                                if (isDynamaxActive) "G-MAX MOVE READY!" else "CHARGED MOVE READY!"
                            } else "Charging ${energyCharge}%",
                            color = if (energyCharge >= 100) PokeDarkBlue else TextMuted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
