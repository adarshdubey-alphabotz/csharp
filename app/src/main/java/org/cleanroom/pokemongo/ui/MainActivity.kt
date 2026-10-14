package org.cleanroom.pokemongo.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cleanroom.pokemongo.data.models.Gym
import org.cleanroom.pokemongo.data.models.Pokestop
import org.cleanroom.pokemongo.data.models.WildPokemonSpawn
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.ui.screens.*
import org.cleanroom.pokemongo.ui.theme.*

sealed class Screen {
    object Map : Screen()
    data class Encounter(val spawn: WildPokemonSpawn) : Screen()
    data class PokestopView(val pokestop: Pokestop) : Screen()
    data class GymView(val gym: Gym) : Screen()
    object PokemonBox : Screen()
    data class PokemonDetail(val pokemonId: String) : Screen()
    object Pokedex : Screen()
    object Eggs : Screen()
    object Buddy : Screen()
    object Bag : Screen()
    object Profile : Screen()
    object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = GameStateRepository.getInstance(applicationContext)

        val initialScreen = when (intent?.getStringExtra("NAVIGATE_TO")) {
            "EGGS" -> Screen.Eggs
            "BUDDY" -> Screen.Buddy
            "PROFILE" -> Screen.Profile
            else -> Screen.Map
        }

        setContent {
            PokemonGoTheme {
                MainAppHost(repo = repo, initialScreen = initialScreen)
            }
        }
    }
}

@Composable
fun MainAppHost(
    repo: GameStateRepository,
    initialScreen: Screen = Screen.Map
) {
    var currentScreen by remember { mutableStateOf(initialScreen) }
    var showMainMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val screen = currentScreen) {
            is Screen.Map -> {
                MapScreen(
                    repo = repo,
                    onEncounterPokemon = { spawn -> currentScreen = Screen.Encounter(spawn) },
                    onOpenPokestop = { stop -> currentScreen = Screen.PokestopView(stop) },
                    onOpenGym = { gym -> currentScreen = Screen.GymView(gym) },
                    onOpenMenu = { showMainMenu = true },
                    onOpenProfile = { currentScreen = Screen.Profile },
                    onOpenBuddy = { currentScreen = Screen.Buddy }
                )
            }
            is Screen.Encounter -> {
                EncounterScreen(
                    spawn = screen.spawn,
                    repo = repo,
                    onCatchComplete = { currentScreen = Screen.Map },
                    onRunAway = { currentScreen = Screen.Map }
                )
            }
            is Screen.PokestopView -> {
                PokestopScreen(
                    pokestop = screen.pokestop,
                    repo = repo,
                    onClose = { currentScreen = Screen.Map }
                )
            }
            is Screen.GymView -> {
                GymScreen(
                    gym = screen.gym,
                    repo = repo,
                    onClose = { currentScreen = Screen.Map }
                )
            }
            is Screen.PokemonBox -> {
                PokemonBoxScreen(
                    repo = repo,
                    onSelectPokemon = { inst -> currentScreen = Screen.PokemonDetail(inst.id) },
                    onBack = { currentScreen = Screen.Map }
                )
            }
            is Screen.PokemonDetail -> {
                PokemonDetailScreen(
                    pokemonId = screen.pokemonId,
                    repo = repo,
                    onBack = { currentScreen = Screen.PokemonBox }
                )
            }
            is Screen.Pokedex -> {
                PokedexScreen(
                    repo = repo,
                    onBack = { currentScreen = Screen.Map }
                )
            }
            is Screen.Eggs -> {
                EggIncubatorScreen(
                    repo = repo,
                    onBack = { currentScreen = Screen.Map }
                )
            }
            is Screen.Buddy -> {
                BuddyScreen(
                    repo = repo,
                    onBack = { currentScreen = Screen.Map }
                )
            }
            is Screen.Bag -> {
                BagScreen(
                    repo = repo,
                    onBack = { currentScreen = Screen.Map }
                )
            }
            is Screen.Profile -> {
                TrainerProfileScreen(
                    repo = repo,
                    onBack = { currentScreen = Screen.Map }
                )
            }
            is Screen.Settings -> {
                SettingsScreen(
                    repo = repo,
                    onBack = { currentScreen = Screen.Map }
                )
            }
        }

        // Radial Main Menu Modal
        if (showMainMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xDD0F172A))
                    .clickable { showMainMenu = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "MAIN MENU",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MenuItem(title = "POKÉDEX", icon = "📖", color = PokemonBlue) {
                            showMainMenu = false
                            currentScreen = Screen.Pokedex
                        }
                        MenuItem(title = "POKÉMON", icon = "🔴", color = GrassGreen) {
                            showMainMenu = false
                            currentScreen = Screen.PokemonBox
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MenuItem(title = "ITEMS", icon = "🎒", color = Color(0xFFF59E0B)) {
                            showMainMenu = false
                            currentScreen = Screen.Bag
                        }
                        MenuItem(title = "EGGS", icon = "🥚", color = PokemonYellow) {
                            showMainMenu = false
                            currentScreen = Screen.Eggs
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MenuItem(title = "BUDDY", icon = "❤️", color = Color(0xFFF43F5E)) {
                            showMainMenu = false
                            currentScreen = Screen.Buddy
                        }
                        MenuItem(title = "SETTINGS", icon = "⚙️", color = Color(0xFF64748B)) {
                            showMainMenu = false
                            currentScreen = Screen.Settings
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    IconButton(
                        onClick = { showMainMenu = false },
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    title: String,
    icon: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 28.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
