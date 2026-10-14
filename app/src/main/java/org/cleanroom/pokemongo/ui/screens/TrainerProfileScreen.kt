package org.cleanroom.pokemongo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cleanroom.pokemongo.data.models.Team
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.ui.theme.*

@Composable
fun TrainerProfileScreen(
    repo: GameStateRepository,
    onBack: () -> Unit
) {
    val trainer by repo.trainerProfile.collectAsState()
    val streaks by repo.dailyStreaks.collectAsState()

    val teamColor = when (trainer.team) {
        Team.MYSTIC -> MysticBlue
        Team.VALOR -> ValorRed
        Team.INSTINCT -> InstinctYellow
        else -> PokemonBlue
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceDark)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Text(
                    text = "TRAINER PROFILE",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Trainer Avatar & Level Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(teamColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Lv.${trainer.level}",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = trainer.name,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = trainer.team.displayName,
                        color = teamColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // XP Progress Bar
                    val xpProgress = (trainer.currentXp.toFloat() / trainer.requiredXpForNextLevel).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { xpProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = AccentCyan,
                        trackColor = Color(0xFF334155)
                    )
                    Text(
                        text = "${trainer.currentXp} / ${trainer.requiredXpForNextLevel} XP",
                        color = TextMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Balances: Stardust & Coins
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${trainer.stardust}", color = StardustPurple, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Text(text = "Total Stardust", fontSize = 11.sp, color = TextMuted)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${trainer.pokeCoins}", color = PokemonYellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Text(text = "PokéCoins", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Daily Streaks 7-Day Tracker
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Streaks (7-Day Bonus)",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("First Catch of the Day", color = TextWhite, fontSize = 13.sp)
                        Text("Day ${streaks.catchStreak} / 7", color = PokemonYellow, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("First PokéStop of the Day", color = TextWhite, fontSize = 13.sp)
                        Text("Day ${streaks.spinStreak} / 7", color = AccentCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Medals & Milestones
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Medals & Records",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    MedalRow("🏃 Jogger", "%.1f km walked".format(trainer.totalKmWalked))
                    MedalRow("🔴 Collector", "${trainer.totalCatches} Pokémon caught")
                    MedalRow("🧬 Scientist", "${trainer.totalEvolutions} Pokémon evolved")
                    MedalRow("🐣 Breeder", "${trainer.totalEggsHatched} Eggs hatched")
                    MedalRow("🎒 Backpacker", "${trainer.totalPokestopsSpun} PokéStops visited")
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun MedalRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(text = subtitle, color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
