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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
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
import org.cleanroom.pokemongo.data.activation.ActivationManager
import org.cleanroom.pokemongo.data.activation.ProFeature
import org.cleanroom.pokemongo.data.automation.AutomationEngine
import org.cleanroom.pokemongo.data.models.PokemonInstance
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.data.repository.PokedexDatabase
import org.cleanroom.pokemongo.ui.theme.*
import kotlin.math.roundToInt

enum class SortMode(val title: String) {
    CP("Combat Power"),
    NUMBER("Pokédex #"),
    RECENT("Recent"),
    FAVORITE("Favorites")
}

@Composable
fun PokemonBoxScreen(
    repo: GameStateRepository,
    onSelectPokemon: (PokemonInstance) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activationManager = remember { ActivationManager.getInstance(context) }
    val automationEngine = remember { AutomationEngine.getInstance(context, repo, activationManager) }
    val automationConfig by automationEngine.config.collectAsState()
    val isIvBadgesUnlocked = activationManager.isFeatureUnlocked(ProFeature.INVENTORY_IV) && automationConfig.inventoryIvBadgesEnabled

    val box by repo.pokemonBox.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(SortMode.CP) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sortedList = remember(box, searchQuery, sortMode) {
        val filtered = box.filter { inst ->
            val species = PokedexDatabase.getById(inst.speciesId)
            val name = inst.nickname ?: species?.name ?: ""
            name.contains(searchQuery, ignoreCase = true)
        }
        when (sortMode) {
            SortMode.CP -> filtered.sortedByDescending { inst ->
                val species = PokedexDatabase.getById(inst.speciesId)
                if (species != null) inst.calculateCp(species) else 0
            }
            SortMode.NUMBER -> filtered.sortedBy { it.speciesId }
            SortMode.RECENT -> filtered.sortedByDescending { it.caughtTimestamp }
            SortMode.FAVORITE -> filtered.sortedByDescending { it.isFavorite }
        }
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
                    text = "Pokémon (${box.size} / 300)",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SurfaceDark)
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort", tint = AccentCyan)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        SortMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.title) },
                                onClick = {
                                    sortMode = mode
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by name...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Grid of Pokémon Cards
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sortedList, key = { it.id }) { inst ->
                    val species = PokedexDatabase.getById(inst.speciesId)
                    val cp = if (species != null) inst.calculateCp(species) else 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectPokemon(inst) },
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val totalIv = inst.ivAttack + inst.ivDefense + inst.ivStamina
                            val ivPct = ((totalIv / 45.0f) * 100.0f).roundToInt()
                            val ivBadgeColor = when {
                                ivPct == 100 -> Color(0xFFF59E0B) // Gold 100%
                                ivPct >= 82 -> Color(0xFF10B981)  // Green
                                ivPct >= 67 -> Color(0xFF3B82F6)  // Blue
                                else -> Color(0xFF64748B)         // Gray
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CP $cp",
                                    color = AccentCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                if (isIvBadgesUnlocked) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ivBadgeColor)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "$ivPct%",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }

                                if (inst.isFavorite) {
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = "Favorite",
                                        tint = PokemonYellow,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Avatar Circle
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(species?.primaryType?.hexColor ?: 0xFF3B82F6).copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (species?.name?.take(2) ?: "PK").uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = inst.nickname ?: species?.name ?: "Pokémon",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )

                            // HP Bar
                            val maxHp = if (species != null) inst.calculateMaxHp(species) else 100
                            LinearProgressIndicator(
                                progress = { (inst.currentHp.toFloat() / maxHp).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF22C55E),
                                trackColor = Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }
    }
}
