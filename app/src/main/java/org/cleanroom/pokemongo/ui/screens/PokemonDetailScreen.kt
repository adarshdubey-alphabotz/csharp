package org.cleanroom.pokemongo.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cleanroom.pokemongo.data.models.PokemonInstance
import org.cleanroom.pokemongo.data.naming.PokemonNameGenerator
import org.cleanroom.pokemongo.data.pvp.PvpLeague
import org.cleanroom.pokemongo.data.pvp.PvpRankCalculator
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.data.repository.PokedexDatabase
import org.cleanroom.pokemongo.ui.theme.*


@Composable
fun PokemonDetailScreen(
    pokemonId: String,
    repo: GameStateRepository,
    onBack: () -> Unit
) {
    val box by repo.pokemonBox.collectAsState()
    val candies by repo.candies.collectAsState()
    val trainer by repo.trainerProfile.collectAsState()

    val pokemon = box.find { it.id == pokemonId }
    if (pokemon == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val species = PokedexDatabase.getById(pokemon.speciesId) ?: return
    val cp = pokemon.calculateCp(species)
    val maxHp = pokemon.calculateMaxHp(species)
    val speciesCandies = candies[species.id] ?: 0

    var showAppraisalModal by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
                    text = "CP $cp",
                    color = AccentCyan,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )

                IconButton(
                    onClick = { repo.toggleFavorite(pokemon.id) },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(SurfaceDark)
                ) {
                    Icon(
                        imageVector = if (pokemon.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (pokemon.isFavorite) PokemonYellow else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CP Arc Gauge Canvas
            Box(
                modifier = Modifier
                    .size(240.dp, 130.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 12f
                    val arcSize = Size(size.width - 40f, size.height * 1.8f)
                    val arcTopLeft = Offset(20f, 20f)

                    // Track Arc
                    drawArc(
                        color = Color(0xFF334155),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )

                    // Level Progress Arc
                    val sweep = (pokemon.level / 40.0f) * 180f
                    drawArc(
                        color = AccentCyan,
                        startAngle = 180f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = arcTopLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                // Pokémon Avatar Center
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color(species.primaryType.hexColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (pokemon.nickname ?: species.name).take(2).uppercase(),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Text(
                text = pokemon.nickname ?: species.name,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "HP ${pokemon.currentHp} / $maxHp",
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )

            // Types Badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(species.primaryType.hexColor))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(text = species.primaryType.displayName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (species.secondaryType != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(species.secondaryType.hexColor))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(text = species.secondaryType.displayName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row: Weight, Level, Height
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "%.1f kg".format(species.weightKg), color = Color.White, fontWeight = FontWeight.Bold)
                    Text(text = "Weight", fontSize = 11.sp, color = TextMuted)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Lv. ${pokemon.level}", color = PokemonYellow, fontWeight = FontWeight.Bold)
                    Text(text = "Level", fontSize = 11.sp, color = TextMuted)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "%.1f m".format(species.heightM), color = Color.White, fontWeight = FontWeight.Bold)
                    Text(text = "Height", fontSize = 11.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stardust and Candy Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${trainer.stardust}", color = StardustPurple, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(text = "Stardust", fontSize = 11.sp, color = TextMuted)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$speciesCandies", color = PokemonYellow, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(text = "${species.name} Candy", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Power Up & Evolve Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Power Up
                Button(
                    onClick = {
                        val success = repo.powerUpPokemon(pokemon.id)
                        actionMessage = if (success) "Power Up Successful!" else "Not enough Stardust or Candy"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PokemonBlue)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("POWER UP", fontWeight = FontWeight.Bold)
                        Text("2500 Stardust + 2 Candy", fontSize = 12.sp, color = PokemonYellow)
                    }
                }

                // Evolve (if has evolution)
                if (species.evolvesToSpeciesId != null) {
                    val nextSpecies = PokedexDatabase.getById(species.evolvesToSpeciesId)
                    Button(
                        onClick = {
                            val evolved = repo.evolvePokemon(pokemon.id)
                            actionMessage = if (evolved != null) "Evolved into ${nextSpecies?.name}!" else "Not enough Candy (${species.candyToEvolve} required)"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GrassGreen)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("EVOLVE (${nextSpecies?.name})", fontWeight = FontWeight.Bold)
                            Text("${species.candyToEvolve} Candy", fontSize = 12.sp, color = PokemonYellow)
                        }
                    }
                }

                // Appraisal & Transfer buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAppraisalModal = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = PokemonYellow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Appraise", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            repo.assignBuddy(pokemon.id)
                            actionMessage = "Set as active Buddy!"
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Set Buddy", color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            val success = repo.transferPokemon(pokemon.id)
                            if (success) onBack()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Text("Transfer", color = Color(0xFFEF4444))
                    }
                }
            }

            // PvP Rank & Pro Rename Card
            val glRank = remember(pokemon) {
                PvpRankCalculator.calculatePvpRank(pokemon.speciesId, pokemon.ivAttack, pokemon.ivDefense, pokemon.ivStamina, PvpLeague.GREAT_LEAGUE)
            }
            val ulRank = remember(pokemon) {
                PvpRankCalculator.calculatePvpRank(pokemon.speciesId, pokemon.ivAttack, pokemon.ivDefense, pokemon.ivStamina, PvpLeague.ULTRA_LEAGUE)
            }

            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PvP League Ranks",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Great League: Rank #${glRank.rank} (CP ${glRank.cpAtLevel})",
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${String.format("%.1f%%", glRank.statProductPercent)}",
                            color = PokemonYellow,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Ultra League: Rank #${ulRank.rank} (CP ${ulRank.cpAtLevel})",
                            color = Color(0xFFA855F7),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${String.format("%.1f%%", ulRank.statProductPercent)}",
                            color = PokemonYellow,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val newName = PokemonNameGenerator.generateName(pokemon)
                            repo.renamePokemon(pokemon.id, newName)
                            actionMessage = "Renamed to '$newName'!"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PokemonBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pro Auto-Rename (IV & Stats)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (actionMessage != null) {

                Text(
                    text = actionMessage ?: "",
                    color = PokemonYellow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // IV Appraisal Modal BottomSheet
        if (showAppraisalModal) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "APPRAISAL",
                        color = PokemonYellow,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Overall rating: ${pokemon.getAppraisalStars()} Stars",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )

                    // Attack Bar (0..15)
                    StatBar(name = "ATTACK", value = pokemon.ivAttack, maxValue = 15)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Defense Bar (0..15)
                    StatBar(name = "DEFENSE", value = pokemon.ivDefense, maxValue = 15)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Stamina Bar (0..15)
                    StatBar(name = "HP / STAMINA", value = pokemon.ivStamina, maxValue = 15)
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showAppraisalModal = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PokemonBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CLOSE")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBar(name: String, value: Int, maxValue: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = name, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = "$value / $maxValue", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { (value.toFloat() / maxValue).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (value == 15) Color(0xFFE11D48) else Color(0xFFFACC15),
            trackColor = Color(0xFF334155)
        )
    }
}
