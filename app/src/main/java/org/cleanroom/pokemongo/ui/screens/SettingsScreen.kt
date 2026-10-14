package org.cleanroom.pokemongo.ui.screens

import android.content.Context
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cleanroom.pokemongo.data.activation.ActivationManager
import org.cleanroom.pokemongo.data.activation.ActivationState
import org.cleanroom.pokemongo.data.activation.ActivationTier
import org.cleanroom.pokemongo.data.activation.ProFeature
import org.cleanroom.pokemongo.data.automation.AutomationEngine
import org.cleanroom.pokemongo.data.models.ItemType
import org.cleanroom.pokemongo.data.repository.GameStateRepository
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

    val activationState by activationManager.activationState.collectAsState()
    val automationConfig by automationEngine.config.collectAsState()

    var isAdventureSyncEnabled by remember { mutableStateOf(false) }
    var isPokemonGoPlusEnabled by remember { mutableStateOf(false) }

    var showActivationDialog by remember { mutableStateOf(false) }
    var inputLicenseKey by remember { mutableStateOf("") }
    var activationErrorText by remember { mutableStateOf<String?>(null) }
    var showBallDropdown by remember { mutableStateOf(false) }

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
                                text = "Pro features (Automation, Joystick, Enhanced Throws, IV HUD) require an active license.",
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
                        subtitle = "Guarantees 100% Excellent throw score with curveball multiplier",
                        checked = automationConfig.enhancedThrowEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(enhancedThrowEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Encounter IV HUD
                    FeatureToggleRow(
                        title = "Encounter IV HUD",
                        subtitle = "Display wild Pokémon IV percentage and exact stats during catch",
                        checked = automationConfig.encounterIvEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(encounterIvEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Caught Preview
                    FeatureToggleRow(
                        title = "Caught Preview",
                        subtitle = "Instant outcome prediction badge before ball shakes complete",
                        checked = automationConfig.caughtPreviewEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(caughtPreviewEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Inventory IV Badges
                    FeatureToggleRow(
                        title = "Inventory IV Badges",
                        subtitle = "Show IV% badges on all Pokémon cards in Box",
                        checked = automationConfig.inventoryIvBadgesEnabled,
                        isUnlocked = isProActive,
                        onCheckedChange = { checked ->
                            automationEngine.updateConfig(automationConfig.copy(inventoryIvBadgesEnabled = checked))
                        },
                        onLockedClick = { showActivationDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. System Services Card
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

                    // Adventure Sync Foreground Service Toggle
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

                    // Pokémon GO Plus Simulator Toggle
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

            // 4. Clean Room Notice Card
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

        // Key Activation Dialog
        if (showActivationDialog) {
            AlertDialog(
                onDismissRequest = { showActivationDialog = false },
                title = {
                    Text("Activate Pro License", fontWeight = FontWeight.Bold, color = Color.White)
                },
                text = {
                    Column {
                        Text(
                            "Enter a valid license key or choose a development trial key below:",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = inputLicenseKey,
                            onValueChange = { inputLicenseKey = it },
                            placeholder = { Text("e.g. DEV-PRO-2026", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        if (activationErrorText != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = activationErrorText ?: "",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Quick Test Keys:", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { inputLicenseKey = "DEV-PRO-2026" },
                                label = { Text("1-Year Pro", fontSize = 11.sp) }
                            )
                            AssistChip(
                                onClick = { inputLicenseKey = "TRIAL-7DAYS" },
                                label = { Text("7-Day Trial", fontSize = 11.sp) }
                            )
                            AssistChip(
                                onClick = { inputLicenseKey = "LIFETIME-PRO" },
                                label = { Text("Lifetime", fontSize = 11.sp) }
                            )
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
                    TextButton(onClick = { showActivationDialog = false }) {
                        Text("Cancel", color = TextMuted)
                    }
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
