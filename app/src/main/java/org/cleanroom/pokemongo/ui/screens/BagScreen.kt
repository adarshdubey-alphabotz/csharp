package org.cleanroom.pokemongo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.cleanroom.pokemongo.data.models.ItemCategory
import org.cleanroom.pokemongo.data.models.ItemType
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.ui.theme.*

@Composable
fun BagScreen(
    repo: GameStateRepository,
    onBack: () -> Unit
) {
    val items by repo.items.collectAsState()
    val totalCount = remember(items) { items.values.sum() }

    var statusMessage by remember { mutableStateOf<String?>(null) }

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
                    text = "ITEM BAG",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "$totalCount / 350",
                    color = AccentCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val healed = repo.healAllPokemon()
                        statusMessage = if (healed > 0) "Restored $healed HP/revives across your Pokémon!" else "All Pokémon are already at 100% HP!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("Heal All (Potions)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                val context = androidx.compose.ui.platform.LocalContext.current
                val activationManager = remember { org.cleanroom.pokemongo.data.activation.ActivationManager.getInstance(context) }
                val automationEngine = remember { org.cleanroom.pokemongo.data.automation.AutomationEngine.getInstance(context, repo, activationManager) }

                Button(
                    onClick = {
                        val report = automationEngine.performAutoDiscard()
                        statusMessage = if (report.totalDiscarded > 0) {
                            "Auto-Discarded ${report.totalDiscarded} excess items!"
                        } else {
                            "Inventory is within limits (no items discarded)"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PokeballRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("Auto-Discard", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (statusMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusMessage ?: "",
                    color = PokemonYellow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))


            // Items List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(ItemType.values()) { itemType ->
                    val count = items[itemType] ?: 0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (itemType.category) {
                                    ItemCategory.BALL -> "⚪"
                                    ItemCategory.POTION -> "🧪"
                                    ItemCategory.REVIVE -> "💎"
                                    ItemCategory.BERRY -> "🍇"
                                    ItemCategory.SPECIAL -> "✨"
                                },
                                fontSize = 28.sp
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = itemType.displayName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = itemType.description,
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 2
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = "x$count",
                                color = if (count > 0) AccentCyan else TextMuted,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
