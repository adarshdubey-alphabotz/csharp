package org.cleanroom.pokemongo.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cleanroom.pokemongo.data.account.AccountManager
import org.cleanroom.pokemongo.data.activation.ActivationManager
import org.cleanroom.pokemongo.data.activation.ActivationState
import org.cleanroom.pokemongo.data.activation.ActivationTier
import org.cleanroom.pokemongo.data.activation.ProFeature
import org.cleanroom.pokemongo.data.automation.AutomationEngine
import org.cleanroom.pokemongo.data.models.ItemType
import org.cleanroom.pokemongo.data.models.Team
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.cleanroom.pokemongo.data.settings.SettingsExporter
import org.cleanroom.pokemongo.service.AdventureSyncService
import org.cleanroom.pokemongo.service.PokemonGoPlusService
import org.cleanroom.pokemongo.ui.theme.*

@Composable
fun SettingsScreen(
    repo: GameStateRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activationManager = remember { ActivationManager.getInstance(context) }
    val automationEngine = remember { AutomationEngine.getInstance(context, repo, activationManager) }
    val accountManager = remember { AccountManager.getInstance(context, repo) }

    val activationState by activationManager.activationState.collectAsState()
    val automationConfig by automationEngine.config.collectAsState()
    val accounts by accountManager.accounts.collectAsState()

    var isAdventureSyncEnabled by remember { mutableStateOf(false) }
    var isPokemonGoPlusEnabled by remember { mutableStateOf(false) }

    var showActivationDialog by remember { mutableStateOf(false) }
    var inputLicenseKey by remember { mutableStateOf("") }
    var activationErrorText by remember { mutableStateOf<String?>(null) }
    var showBallDropdown by remember { mutableStateOf(false) }
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showFeedLanguageDropdown by remember { mutableStateOf(false) }

    var showAccountsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }

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
                    text = "SETTINGS & LICENSING",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. License & Pro Activation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Product Licensing",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        val isPro = activationState is ActivationState.Active && !(activationState as ActivationState.Active).isExpired()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPro) Color(0xFF10B981) else Color(0xFF64748B))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isPro) "PRO ACTIVATED" else "FREE TIER",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    when (val state = activationState) {
                        is ActivationState.Active -> {
                            Text(
                                text = "Key: ${state.licenseKey}",
                                color = AccentCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val remaining = state.remainingDays()
                            Text(
                                text = if (remaining >= 9000L) "License Duration: Lifetime Unlocked" else "Valid for: $remaining day(s)",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { activationManager.deactivate() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Deactivate License", fontWeight = FontWeight.Bold)
                            }
                        }
                        is ActivationState.Inactive -> {
                            Text(
                                text = "Pro features (Automation, Joystick, Enhanced Throws, Sniper, IV HUD) require an active license.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    activationErrorText = null
                                    inputLicenseKey = ""
                                    showActivationDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PokemonBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Activate Pro Key", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Pro Feature Access & Automation Card
            val isProActive = activationManager.isProActive()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Movement & Automation Controls",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isProActive) {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = PokemonYellow, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Virtual Joystick Toggle
                    FeatureToggleRow(
                        title = "Virtual Joystick",
                        subtitle = "On-screen floating thumbstick for manual walking",
                        checked = automationConfig.joystickEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(joystickEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Speed-Up Controls Toggle
                    FeatureToggleRow(
                        title = "Speed-Up Controls",
                        subtitle = "Switch between 9.3km/h, 18.5km/h, 30km/h, 60km/h",
                        checked = automationConfig.speedControlsEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(speedControlsEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-Catch Engine
                    FeatureToggleRow(
                        title = "Auto-Catch Engine",
                        subtitle = "Automatically catch wild Pokémon within 80 meters",
                        checked = automationConfig.autoCatchEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(autoCatchEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )

                    // Preferred Ball Selector (when Auto-Catch is on)
                    if (automationConfig.autoCatchEnabled && isProActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBackground.copy(alpha = 0.6f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Preferred Ball:", color = TextMuted, fontSize = 12.sp)
                            Box {
                                Button(
                                    onClick = { showBallDropdown = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(automationConfig.preferredBall.displayName, fontSize = 12.sp, color = AccentCyan)
                                }
                                DropdownMenu(
                                    expanded = showBallDropdown,
                                    onDismissRequest = { showBallDropdown = false }
                                ) {
                                    listOf(ItemType.POKE_BALL, ItemType.GREAT_BALL, ItemType.ULTRA_BALL).forEach { ball ->
                                        DropdownMenuItem(
                                            text = { Text(ball.displayName) },
                                            onClick = {
                                                automationEngine.updateConfig(automationConfig.copy(preferredBall = ball))
                                                showBallDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-Spin PokéStops
                    FeatureToggleRow(
                        title = "Auto-Spin PokéStops",
                        subtitle = "Automatically spin in-range PokéStops (within 40m)",
                        checked = automationConfig.autoSpinEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(autoSpinEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-Feed Buddy
                    FeatureToggleRow(
                        title = "Auto-Feed Buddy",
                        subtitle = "Auto-feed berries when buddy hunger falls below 30%",
                        checked = automationConfig.autoFeedEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(autoFeedEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Enhanced Throw
                    FeatureToggleRow(
                        title = "Enhanced Throw",
                        subtitle = "Overrides throw scoring (Nice/Great/Excellent), curveball & AR+ Expert Handler",
                        checked = automationConfig.enhancedThrowEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(enhancedThrowEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )

                    if (automationConfig.enhancedThrowEnabled && isProActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBackground.copy(alpha = 0.6f))
                                .padding(10.dp)
                        ) {
                            Text("Throw Quality Target:", color = TextMuted, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(
                                    org.cleanroom.pokemongo.data.repository.ThrowQuality.NICE,
                                    org.cleanroom.pokemongo.data.repository.ThrowQuality.GREAT,
                                    org.cleanroom.pokemongo.data.repository.ThrowQuality.EXCELLENT
                                ).forEach { quality ->
                                    val isSelected = automationConfig.enhancedThrowQuality == quality
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { automationEngine.updateConfig(automationConfig.copy(enhancedThrowQuality = quality)) },
                                        label = { Text(quality.displayName, fontSize = 11.sp) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Guaranteed Curveball", color = TextWhite, fontSize = 12.sp)
                                Switch(
                                    checked = automationConfig.guaranteedCurveball,
                                    onCheckedChange = { automationEngine.updateConfig(automationConfig.copy(guaranteedCurveball = it)) }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("AR+ Expert Handler (+300 XP, 1.25x)", color = TextWhite, fontSize = 12.sp)
                                Switch(
                                    checked = automationConfig.arPlusBonusEnabled,
                                    onCheckedChange = { automationEngine.updateConfig(automationConfig.copy(arPlusBonusEnabled = it)) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Catch
                    FeatureToggleRow(
                        title = "Quick Catch",
                        subtitle = "Instantly skips 3-shake ball animation and adds caught Pokémon",
                        checked = automationConfig.quickCatchEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(quickCatchEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Block Non-Shiny
                    FeatureToggleRow(
                        title = "Block Non-Shiny",
                        subtitle = "Automatically blocks or flees encounters that are not shiny",
                        checked = automationConfig.blockNonShinyEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(blockNonShinyEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Block Non-Hundo
                    FeatureToggleRow(
                        title = "Block Non-Hundo",
                        subtitle = "Automatically blocks encounters with IV less than 100%",
                        checked = automationConfig.blockNonHundoEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(blockNonHundoEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-Discard Surplus Items
                    FeatureToggleRow(
                        title = "Auto-Discard Items",
                        subtitle = "Automatically cleans inventory exceeding keep limits",
                        checked = automationConfig.autoDiscardEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(autoDiscardEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Transfer on Catch
                    FeatureToggleRow(
                        title = "Transfer on Catch",
                        subtitle = "Auto-transfers caught Pokémon below 90% IV with keep-protection rules",
                        checked = automationConfig.transferOnCatchEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(transferOnCatchEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )

                    if (automationConfig.transferOnCatchEnabled && isProActive) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBackground.copy(alpha = 0.6f))
                                .padding(10.dp)
                        ) {
                            Text("Keep Protection Rules:", color = TextMuted, fontSize = 11.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Keep Legendaries / Mythicals", color = TextWhite, fontSize = 12.sp)
                                Switch(
                                    checked = automationConfig.keepLegendaryOnCatch,
                                    onCheckedChange = { automationEngine.updateConfig(automationConfig.copy(keepLegendaryOnCatch = it)) }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Keep Top 50 PvP Ranks (GL/UL)", color = TextWhite, fontSize = 12.sp)
                                Switch(
                                    checked = automationConfig.keepPvPRankOnCatch,
                                    onCheckedChange = { automationEngine.updateConfig(automationConfig.copy(keepPvPRankOnCatch = it)) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-Incubator
                    FeatureToggleRow(
                        title = "Auto-Incubator",
                        subtitle = "Auto-assigns eggs (infinite and super) by priority",
                        checked = automationConfig.autoIncubatorEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(autoIncubatorEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )

                    if (automationConfig.autoIncubatorEnabled && isProActive) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBackground.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Incubator Priority:", color = TextMuted, fontSize = 12.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = automationConfig.eggIncubatorPriority == "SHORTEST_FIRST",
                                    onClick = { automationEngine.updateConfig(automationConfig.copy(eggIncubatorPriority = "SHORTEST_FIRST")) },
                                    label = { Text("Shortest (2km)", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = automationConfig.eggIncubatorPriority == "LONGEST_FIRST",
                                    onClick = { automationEngine.updateConfig(automationConfig.copy(eggIncubatorPriority = "LONGEST_FIRST")) },
                                    label = { Text("Longest (12km)", fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Gift Assistant
                    FeatureToggleRow(
                        title = "Gift Assistant",
                        subtitle = "Batch opens and sends daily friend gifts in one click",
                        checked = automationConfig.giftAssistantEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(giftAssistantEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quest Assistant
                    FeatureToggleRow(
                        title = "Quest Assistant",
                        subtitle = "Automatically triggers snapshot and interaction tasks for research",
                        checked = automationConfig.questAssistantEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(questAssistantEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Spawn Booster Multiplier
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Spawn Booster Density", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Cluster multiplier: ${automationConfig.spawnBoosterMultiplier}x wild spawns", color = TextMuted, fontSize = 11.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 2, 3).forEach { mult ->
                                val isSelected = automationConfig.spawnBoosterMultiplier == mult
                                Button(
                                    onClick = {
                                        if (isProActive) {
                                            automationEngine.updateConfig(automationConfig.copy(spawnBoosterMultiplier = mult))
                                            repo.refreshWildSpawns(mult)
                                        } else {
                                            showActivationDialog = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) PokemonBlue else SurfaceDark
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("${mult}x", fontSize = 11.sp, color = if (isSelected) Color.White else TextMuted)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hide Trainer Name
                    FeatureToggleRow(
                        title = "Hide Trainer Name",
                        subtitle = "Streamer privacy mode: masks username with 'Trainer'",
                        checked = automationConfig.hideTrainerName,
                        isUnlocked = true, // Free privacy feature
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(hideTrainerName = checked))
                        },
                        onLockedClick = { }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hide App with Gesture Controls
                    FeatureToggleRow(
                        title = "Hide App with Gesture Controls",
                        subtitle = "Long press on map canvas hides all mod overlays (Stealth Mode)",
                        checked = automationConfig.hideAppWithGestureEnabled,
                        isUnlocked = true, // Free privacy feature
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(hideAppWithGestureEnabled = checked))
                        },
                        onLockedClick = { }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom Floating Shortcuts Config
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBackground.copy(alpha = 0.6f))
                            .padding(10.dp)
                    ) {
                        Text("Custom Shortcuts on Map Toolbar:", color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        val availableShortcuts = listOf(
                            "SNIPER" to "Sniper",
                            "ROUTES" to "Routes",
                            "TOOLS" to "Tools",
                            "HEAL" to "Heal",
                            "SPAWNS" to "Spawns",
                            "RESET" to "Reset UI",
                            "STEALTH" to "Stealth"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            availableShortcuts.take(4).forEach { (id, label) ->
                                val active = automationConfig.customShortcuts.contains(id)
                                FilterChip(
                                    selected = active,
                                    onClick = {
                                        val newShortcuts = if (active) {
                                            automationConfig.customShortcuts - id
                                        } else {
                                            automationConfig.customShortcuts + id
                                        }
                                        automationEngine.updateConfig(automationConfig.copy(customShortcuts = newShortcuts))
                                    },
                                    label = { Text(label, fontSize = 9.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            availableShortcuts.drop(4).forEach { (id, label) ->
                                val active = automationConfig.customShortcuts.contains(id)
                                FilterChip(
                                    selected = active,
                                    onClick = {
                                        val newShortcuts = if (active) {
                                            automationConfig.customShortcuts - id
                                        } else {
                                            automationConfig.customShortcuts + id
                                        }
                                        automationEngine.updateConfig(automationConfig.copy(customShortcuts = newShortcuts))
                                    },
                                    label = { Text(label, fontSize = 9.sp) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Accounts & Settings Data Management Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Accounts & Configuration Data",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Accounts Switcher Row
                    Button(
                        onClick = { showAccountsDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark.copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Accounts Manager (${accounts.size} profiles)", fontWeight = FontWeight.Bold, color = AccentCyan)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                exportedJsonText = SettingsExporter.exportToJson(automationConfig)
                                showExportDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON", fontSize = 12.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                importStatusMessage = null
                                importJsonInput = ""
                                showImportDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark.copy(alpha = 0.8f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import JSON", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color(0xFF334155), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Language & Interface Options",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // App Language Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("App Language:", color = TextMuted, fontSize = 12.sp)
                        Box {
                            Button(
                                onClick = { showLanguageDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark.copy(alpha = 0.8f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(automationConfig.appLanguage, fontSize = 12.sp, color = AccentCyan)
                            }
                            DropdownMenu(
                                expanded = showLanguageDropdown,
                                onDismissRequest = { showLanguageDropdown = false }
                            ) {
                                listOf("English", "日本語 (Japanese)", "Deutsch", "Español", "Français", "中文 (Chinese)").forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang) },
                                        onClick = {
                                            automationEngine.updateConfig(automationConfig.copy(appLanguage = lang))
                                            showLanguageDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Feed Language Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pokémon Feed Language:", color = TextMuted, fontSize = 12.sp)
                        Box {
                            Button(
                                onClick = { showFeedLanguageDropdown = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark.copy(alpha = 0.8f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(automationConfig.pokemonFeedLanguage, fontSize = 12.sp, color = PokemonYellow)
                            }
                            DropdownMenu(
                                expanded = showFeedLanguageDropdown,
                                onDismissRequest = { showFeedLanguageDropdown = false }
                            ) {
                                listOf("English", "Japanese", "German", "French").forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang) },
                                        onClick = {
                                            automationEngine.updateConfig(automationConfig.copy(pokemonFeedLanguage = lang))
                                            showFeedLanguageDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reset UI & Movement Controls Button
                    Button(
                        onClick = {
                            val movementController = org.cleanroom.pokemongo.data.movement.MovementController.getInstance(repo)
                            movementController.resetMovementAndUi()
                            repo.resetUiState()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626).copy(alpha = 0.85f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset UI, Joystick & Waypoints", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. System Services Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Background Services",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Adventure Sync", color = TextWhite, fontWeight = FontWeight.SemiBold)
                            Text("Track distance and hatch eggs while app is closed", color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isAdventureSyncEnabled,
                            onCheckedChange = { checked ->
                                isAdventureSyncEnabled = checked
                                if (checked) {
                                    AdventureSyncService.start(context)
                                } else {
                                    AdventureSyncService.stop(context)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pokémon GO Plus (BLE Companion)", color = TextWhite, fontWeight = FontWeight.SemiBold)
                            Text("Background accessory auto-catcher service", color = TextMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isPokemonGoPlusEnabled,
                            onCheckedChange = { checked ->
                                isPokemonGoPlusEnabled = checked
                                if (checked) {
                                    PokemonGoPlusService.start(context)
                                } else {
                                    PokemonGoPlusService.stop(context)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Clean Room Notice Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Clean-Room Implementation Notice",
                        color = PokemonYellow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This application was independently engineered based on reverse-engineering analysis of APK v0.433.0. Built natively with modern Kotlin and Jetpack Compose. Does not contain proprietary Niantic backend credentials or reverse-engineered source code.",
                        color = TextWhite,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Dialog: Key Activation
        if (showActivationDialog) {
            AlertDialog(
                onDismissRequest = { showActivationDialog = false },
                title = { Text("Activate Pro License", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column {
                        Text("Enter a valid license key or choose a development trial key below:", color = TextMuted, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inputLicenseKey,
                            onValueChange = { inputLicenseKey = it },
                            placeholder = { Text("e.g. DEV-PRO-2026", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (activationErrorText != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = activationErrorText ?: "", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Quick Test Keys:", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = { inputLicenseKey = "DEV-PRO-2026" }, label = { Text("1-Year Pro", fontSize = 11.sp) })
                            AssistChip(onClick = { inputLicenseKey = "TRIAL-7DAYS" }, label = { Text("7-Day Trial", fontSize = 11.sp) })
                            AssistChip(onClick = { inputLicenseKey = "LIFETIME-PRO" }, label = { Text("Lifetime", fontSize = 11.sp) })
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val res = activationManager.activate(inputLicenseKey)
                            if (res.isSuccess) {
                                showActivationDialog = false
                                activationErrorText = null
                            } else {
                                activationErrorText = res.exceptionOrNull()?.message ?: "Invalid key"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PokemonBlue)
                    ) {
                        Text("Activate", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showActivationDialog = false }) { Text("Cancel", color = TextMuted) }
                },
                containerColor = SurfaceDark
            )
        }

        // Dialog: Accounts Manager
        if (showAccountsDialog) {
            AlertDialog(
                onDismissRequest = { showAccountsDialog = false },
                title = { Text("Accounts Manager", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        Text("Switch active trainer profile:", color = TextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(accounts) { acc ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (acc.isCurrent) Color(0xFF1E3A8A) else Color(0xFF1E293B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(acc.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                if (acc.isCurrent) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("(ACTIVE)", color = PokemonYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Text("Level ${acc.level} | ${acc.team.displayName}", color = TextMuted, fontSize = 11.sp)
                                        }
                                        if (!acc.isCurrent) {
                                            Button(
                                                onClick = {
                                                    accountManager.switchAccount(acc.id)
                                                    showAccountsDialog = false
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("Switch", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAccountsDialog = false }) { Text("Done", color = TextMuted) }
                },
                containerColor = SurfaceDark
            )
        }

        // Dialog: Export Settings JSON
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("Export Settings JSON", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        Text("Current settings formatted as JSON:", color = TextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = exportedJsonText,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { showExportDialog = false }) { Text("Done") }
                },
                containerColor = SurfaceDark
            )
        }

        // Dialog: Import Settings JSON
        if (showImportDialog) {
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Import Settings JSON", fontWeight = FontWeight.Bold, color = Color.White) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                        Text("Paste your exported configuration JSON:", color = TextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = importJsonInput,
                            onValueChange = { importJsonInput = it },
                            placeholder = { Text("Paste JSON here...", color = TextMuted) },
                            modifier = Modifier.fillMaxWidth().height(160.dp)
                        )
                        if (importStatusMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = importStatusMessage ?: "", color = Color(0xFFEF4444), fontSize = 11.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val res = SettingsExporter.importFromJson(importJsonInput)
                            if (res.isSuccess) {
                                automationEngine.updateConfig(res.getOrThrow())
                                showImportDialog = false
                            } else {
                                importStatusMessage = res.exceptionOrNull()?.message ?: "Invalid JSON"
                            }
                        }
                    ) {
                        Text("Import & Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) { Text("Cancel", color = TextMuted) }
                },
                containerColor = SurfaceDark
            )
        }
    }
}

@Composable
private fun FeatureToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    isUnlocked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onLockedClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!isUnlocked) {
                    onLockedClick()
                } else {
                    onCheckedChange(!checked)
                }
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = TextWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (!isUnlocked) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("(PRO)", color = PokemonYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
        }
        Switch(
            checked = checked && isUnlocked,
            onCheckedChange = { isChecked ->
                if (!isUnlocked) {
                    onLockedClick()
                } else {
                    onCheckedChange(isChecked)
                }
            },
            enabled = isUnlocked
        )
    }
}
