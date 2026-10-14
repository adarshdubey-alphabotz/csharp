package org.cleanroom.pokemongo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cleanroom.pokemongo.data.models.ItemType
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.data.repository.PokedexDatabase
import org.cleanroom.pokemongo.ui.theme.*

@Composable
fun BuddyScreen(
    repo: GameStateRepository,
    onBack: () -> Unit
) {
    val buddy by repo.buddy.collectAsState()
    val box by repo.pokemonBox.collectAsState()
    val items by repo.items.collectAsState()

    val buddyPokemon = box.find { it.id == buddy.pokemonInstanceId }
    val species = if (buddyPokemon != null) PokedexDatabase.getById(buddyPokemon.speciesId) else null

    var feedbackText by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
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
                    text = "BUDDY ADVENTURE",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Buddy Avatar Showcase Card
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
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color(species?.primaryType?.hexColor ?: 0xFF3B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (species?.name?.take(2) ?: "PK").uppercase(),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = buddyPokemon?.nickname ?: species?.name ?: "No Buddy Selected",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${buddy.getAffectionLevelName()} • Emotion: ${buddy.emotion.title}",
                        color = PokemonYellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hunger Meter
                    Text(
                        text = "Hunger Level (${buddy.hungerPercent}%)",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    LinearProgressIndicator(
                        progress = { (buddy.hungerPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .padding(top = 4.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (buddy.hungerPercent > 50) Color(0xFF22C55E) else Color(0xFFFACC15),
                        trackColor = Color(0xFF334155)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Candy Distance: %.1f / %.1f km (Total: %d candies)".format(
                            buddy.affectionKmProgress,
                            species?.buddyKmPerCandy ?: 3.0f,
                            buddy.totalCandiesFound
                        ),
                        color = AccentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: Feed Berry & Play
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val razzCount = items[ItemType.RAZZ_BERRY] ?: 0
                        if (razzCount > 0) {
                            repo.feedBuddy(ItemType.RAZZ_BERRY)
                            feedbackText = "Fed Razz Berry! Buddy is delighted!"
                        } else {
                            feedbackText = "No Razz Berries in inventory!"
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PokemonBlue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("🍓 FEED BERRY")
                }

                Button(
                    onClick = {
                        val played = repo.playWithBuddy()
                        feedbackText = if (played) "Played with buddy! +1 Heart" else "Already played with buddy today!"
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GrassGreen),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("✨ PLAY TOGETHER")
                }
            }

            if (feedbackText != null) {
                Text(
                    text = feedbackText ?: "",
                    color = PokemonYellow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Daily Hearts Checklist
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Daily Activities (${buddy.getTotalHeartsToday()} / 8 Hearts)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    HeartRow("Walk together", buddy.heartsWalk, 3)
                    HeartRow("Give your buddy a treat", buddy.heartsFeed, 3)
                    HeartRow("Play together", buddy.heartsPlay, 1)
                    HeartRow("Battle together", buddy.heartsBattle, 1)
                }
            }
        }
    }
}

@Composable
private fun HeartRow(activity: String, count: Int, max: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = activity, color = TextWhite, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in 0 until max) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = if (i < count) Color(0xFFF43F5E) else Color(0xFF475569),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
