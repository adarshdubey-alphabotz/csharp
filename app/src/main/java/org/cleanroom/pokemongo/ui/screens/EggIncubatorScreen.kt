package org.cleanroom.pokemongo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cleanroom.pokemongo.data.models.Egg
import org.cleanroom.pokemongo.data.models.EggTier
import org.cleanroom.pokemongo.data.models.PokemonInstance
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.data.repository.PokedexDatabase
import org.cleanroom.pokemongo.ui.theme.*

@Composable
fun EggIncubatorScreen(
    repo: GameStateRepository,
    onBack: () -> Unit
) {
    val eggs by repo.eggs.collectAsState()
    val incubators by repo.incubators.collectAsState()
    val pendingHatch by repo.pendingHatchEgg.collectAsState()

    var selectedEggForIncubation by remember { mutableStateOf<Egg?>(null) }
    var hatchedPokemonResult by remember { mutableStateOf<Pair<PokemonInstance, EggTier>?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
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
                    text = "EGGS & INCUBATORS",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Incubators Carousel Header
            Text(
                text = "Active Incubators (${incubators.count { it.currentEggId != null }} / ${incubators.size})",
                color = AccentCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                incubators.forEach { inc ->
                    val eggInInc = eggs.find { it.id == inc.currentEggId }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (inc.isUnlimited) "Infinite (1.0x)" else "Super (0.67x)",
                                color = if (inc.isUnlimited) PokemonYellow else Color(0xFFF43F5E),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (eggInInc != null) "🥚" else "🔓",
                                fontSize = 28.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            if (eggInInc != null) {
                                Text(
                                    text = "%.1f / %.1f km".format(eggInInc.walkedKm, eggInInc.tier.distanceKm),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = if (inc.isUnlimited) "Ready" else "${inc.usesRemaining} uses left",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Eggs List Header
            Text(
                text = "Eggs Inventory (${eggs.size} / 9)",
                color = AccentCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(eggs, key = { it.id }) { egg ->
                    val isIncubating = egg.incubatorId != null
                    val readyToHatch = egg.isReadyToHatch()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (readyToHatch) {
                                    val res = repo.hatchEgg(egg.id)
                                    hatchedPokemonResult = res
                                } else if (!isIncubating) {
                                    selectedEggForIncubation = egg
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🥚", fontSize = 32.sp)

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${egg.tier.distanceKm.toInt()} km Egg",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                if (isIncubating) {
                                    LinearProgressIndicator(
                                        progress = { (egg.walkedKm / egg.tier.distanceKm).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (readyToHatch) PokemonYellow else AccentCyan,
                                        trackColor = Color(0xFF334155)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (readyToHatch) "Ready to Hatch! (Tap to Hatch)" else "%.1f / %.1f km".format(egg.walkedKm, egg.tier.distanceKm),
                                        color = if (readyToHatch) PokemonYellow else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "Not incubating (Tap to incubate)",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modal: Select Incubator for Egg
        if (selectedEggForIncubation != null) {
            val egg = selectedEggForIncubation!!
            val availableIncubators = incubators.filter { it.isAvailable() }

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
                        text = "INCUBATE EGG",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Select an available incubator for your ${egg.tier.distanceKm.toInt()}km egg:",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                    )

                    if (availableIncubators.isEmpty()) {
                        Text(
                            text = "No incubators currently available. Free up an incubator by walking!",
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp
                        )
                    } else {
                        availableIncubators.forEach { inc ->
                            Button(
                                onClick = {
                                    repo.placeEggInIncubator(egg.id, inc.id)
                                    selectedEggForIncubation = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (inc.isUnlimited) PokemonBlue else Color(0xFFE11D48)
                                )
                            ) {
                                Text(
                                    text = if (inc.isUnlimited) "Use Infinite Incubator (1.0x)" else "Use Super Incubator (0.67x)",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { selectedEggForIncubation = null }) {
                        Text("CANCEL", color = TextMuted)
                    }
                }
            }
        }

        // Modal: Hatched Pokémon Reveal ("Oh?")
        if (hatchedPokemonResult != null) {
            val (pokemon, tier) = hatchedPokemonResult!!
            val species = PokedexDatabase.getById(pokemon.speciesId)

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
                        text = "OH?",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = PokemonYellow
                    )
                    Text(
                        text = "Your ${tier.distanceKm.toInt()}km Egg hatched into ${species?.name}!",
                        fontSize = 15.sp,
                        color = Color.White,
                        modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(species?.primaryType?.hexColor ?: 0xFF3B82F6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (species?.name?.take(2) ?: "PK").uppercase(),
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "CP ${pokemon.calculateCp(species!!)}",
                        color = AccentCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )

                    Text(
                        text = "+16 Candies & +1200 Stardust awarded!",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Button(
                        onClick = { hatchedPokemonResult = null },
                        colors = ButtonDefaults.buttonColors(containerColor = PokemonBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("AWESOME!", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
