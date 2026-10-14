package org.cleanroom.pokemongo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cleanroom.pokemongo.data.models.PokemonSpecies
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.data.repository.PokedexDatabase
import org.cleanroom.pokemongo.ui.theme.*

@Composable
fun PokedexScreen(
    repo: GameStateRepository,
    onBack: () -> Unit
) {
    val box by repo.pokemonBox.collectAsState()
    val caughtSpeciesIds = remember(box) { box.map { it.speciesId }.toSet() }
    val allSpecies = remember { PokedexDatabase.speciesList }

    var selectedSpecies by remember { mutableStateOf<PokemonSpecies?>(null) }

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
                    text = "POKÉDEX",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.size(40.dp))
            }

            // Summary Stats Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${caughtSpeciesIds.size}", color = AccentCyan, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(text = "Caught", fontSize = 11.sp, color = TextMuted)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${allSpecies.size}", color = PokemonYellow, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(text = "Seen", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }

            // Pokédex Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(allSpecies, key = { it.id }) { species ->
                    val isCaught = caughtSpeciesIds.contains(species.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { selectedSpecies = species },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCaught) SurfaceDark else Color(0xFF1E293B).copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "#%03d".format(species.id),
                                color = if (isCaught) AccentCyan else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCaught) Color(species.primaryType.hexColor) else Color(0xFF334155)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isCaught) species.name.take(2).uppercase() else "?",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (isCaught) species.name else "???",
                                color = if (isCaught) Color.White else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Species Detail Modal
        if (selectedSpecies != null) {
            val spec = selectedSpecies!!
            val isCaught = caughtSpeciesIds.contains(spec.id)

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#%03d %s".format(spec.id, if (isCaught) spec.name else "Unknown"),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        IconButton(onClick = { selectedSpecies = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(spec.primaryType.hexColor))
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(text = spec.primaryType.displayName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (spec.secondaryType != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(spec.secondaryType.hexColor))
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text(text = spec.secondaryType.displayName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = spec.flavorText,
                        color = TextWhite,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "HT: %.1f m".format(spec.heightM), color = Color.White, fontSize = 12.sp)
                            Text(text = "WT: %.1f kg".format(spec.weightKg), color = Color.White, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Base Atk: ${spec.baseAttack}", color = Color(0xFFF87171), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Base Def: ${spec.baseDefense}", color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { selectedSpecies = null },
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
