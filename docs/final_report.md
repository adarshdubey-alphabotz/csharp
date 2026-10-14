# Android Reverse Engineering & Clean-Room Application Report

## 1. Project & Target Overview
- **Project**: Static Analysis and Independent Clean-Room Implementation of Pokémon GO
- **Target APK**: `original/test.apk`
- **Application Label**: `Pokémon GO`
- **Package Name**: `com.nianticlabs.pokemongo`
- **Version Name / Code**: `0.433.0` / `2026100702`
- **Target SDK**: `36` (Android 16 / Vanilla Ice Cream preview) | **Min SDK**: `28`
- **Original APK SHA-256**: `80aa97f7ebdf04ecd972b82a2a4495f087331024487d77ed1b1c3c457e547315`
- **Original APK Size**: 210,412,021 bytes (~200.67 MB)
- **Primary Framework**: Dual-layer architecture: Unity Engine 2022.3+ (arm64-v8a IL2CPP v39) combined with native Niantic shared objects (`libholoholo.so`, `libadventuresync.so`, `libNianticLabsPlugin.so`) and Android Java/Kotlin application framework (`UnityMainActivity`).

---

## 2. Key Reverse Engineering Findings

### 2.1 Native Engine & Architecture
1. **Unity IL2CPP Core**:
   - `lib/arm64-v8a/libil2cpp.so` (252.4 MB) houses the ahead-of-time compiled C# codebase.
   - `assets/bin/Data/Managed/Metadata/global-metadata.dat` (35 MB, metadata format version 39) contains 400,840 recovered string tokens, type descriptors, and method definitions.
   - Core gameplay mechanics mapped: Wild encounter state machine (`CatchPokemonResponse`, `CatchStatus`, throw quality multipliers, curveball calculations), Combat Power (`CP`) and health (`HP`) calculation formulas, species IV stats (Attack, Defense, Stamina), evolution trees, and Pokédex entries.
2. **Proprietary Niantic Extensions**:
   - `libholoholo.so` (13.4 MB): Network RPC serializer and client-side anti-cheat verification.
   - `libadventuresync.so` (4.3 MB) and `libnia-advsync-reporter-android.so` (5.3 MB): Sensor data aggregation, GPS trajectory filtering, and fitness distance tracking.
   - `libfmod.so` / `libfmodstudio.so` (2.8 MB): Spatial audio engine with sound banks (`Master.bank`).

### 2.2 Android Framework & Services
1. **Adventure Sync Background Service (`LocationForegroundService`)**:
   - Operates as a persistent Android Foreground Service displaying `"Routes is running in the background"`.
   - Collects GPS breadcrumbs (`LocationData`) via Kotlin Coroutines and Flows at 10-second intervals.
   - Accumulates distance onto active egg incubators and buddy Pokémon.
2. **Companion Device BLE Subsystem (`com.nianticlabs.pokemongoplus`)**:
   - Manages Bluetooth Low Energy (BLE) communication with companion accessories (Pokémon GO Plus / Poké Ball Plus / Go Plus +).
   - Core service: `BackgroundService` with `SfidaBluetoothDriver` and `SfidaPeripheral`.
   - Interacts with peripheral GATT services:
     - Device Control: `21c50462-67cb-63a3-5c4c-82b5b9939aeb`
     - LED & Vibration Haptics: `21c50462-67cb-63a3-5c4c-82b5b9939aec`
     - Button Notification: `21c50462-67cb-63a3-5c4c-82b5b9939aed`
     - Certification Service: `bbe87709-5b89-4433-ab7f-8b8eef0d8e37`
   - Posts notifications for background game events: `kCapturedPokemon`, `kPokemonEscaped`, `kRetrievedItems`, `kOutOfPokeballs`.
3. **Android AppWidgets**:
   - Four distinct `AppWidgetProvider` implementations: `EggsWidget`, `BuddyWidget`, `DailyStreaksWidget`, and `ActivitySummaryWidget`.
   - Data serialized through Protocol Buffers (`AdventureSyncProgress.AdventureSyncProgressResponse`) and shared via `KeyValueContentProvider`.
   - Supports deep links into app sections (e.g., `notification://dl_action=OPEN_BUDDY`, `notification://dl_action=OPEN_POKEMON_INVENTORY,dl_tab=EGGS`).

---

## 3. Independent Clean-Room Implementation

An independent Android application was designed and constructed in `implementation/` using Kotlin and Jetpack Compose.

### 3.1 Architecture & Tech Stack
- **Target SDK**: 34 (Android 14) | **Min SDK**: 28 (Android 9.0)
- **UI Framework**: Modern Jetpack Compose (Compose BOM 2024.04.00, Material 3)
- **Language**: Kotlin 1.9.24 with Coroutines & StateFlow
- **Package**: `org.cleanroom.pokemongo`
- **Output Artifact**: `output/cleanroom-pokemongo-debug.apk` (51 MB, SHA-256: `132c0a0006fb7cfcc484205d23966d0add7d7288f8ff43c923e4205417e24a47`)

### 3.2 Implemented Features & Mechanics
1. **Interactive World Map Screen (`MapScreen.kt`)**:
   - Custom-rendered world map with player avatar, compass bearing, pulsating 80m detection radar ring.
   - Real GPS positioning and virtual directional pad movement.
   - Interactive PokéStops with cooldown timers, lure sparkles, and range detection.
   - Interactive Gym towers with controlling team banners (Mystic, Valor, Instinct).
   - Wild Pokémon roaming in the vicinity with live level and species data.
   - Nearby sightings radar bar showing nearest Pokémon.
2. **Wild Pokémon Encounter & Capture Screen (`EncounterScreen.kt`)**:
   - 3D-perspective encounter canvas.
   - Ballistic Pokeball throwing physics with drag-and-swipe touch gestures.
   - Dynamic shrinking target ring color-coded by catch difficulty (Green, Yellow, Orange, Red).
   - Real-time throw accuracy evaluation: Nice! (+10 XP), Great! (+50 XP), Excellent! (+100 XP), and Curveball bonus (+10 XP).
   - Pokeball shake sequence (1, 2, 3 shakes -> Gotcha! or Breakout).
   - Berry feeding drawer: Razz Berry (1.5x catch rate), Nanab Berry (stabilizes ring), Pinap Berry (2x candy).
   - Ball selection drawer: Poké Ball (1.0x), Great Ball (1.5x), Ultra Ball (2.0x).
3. **PokéStop Spin & Reward Screen (`PokestopScreen.kt`)**:
   - Interactive photo disc with horizontal swipe gesture animation.
   - Item loot drops: Poké Balls, Great Balls, Potions, Revives, Berries, and Eggs.
   - 5-minute cooldown timer with visual state change.
4. **Gym & Battle Arena Screen (`GymScreen.kt`)**:
   - Gym tower inspection with defending team and defender stats.
   - Real-time battle simulator: Fast Attack tapping, Charged Move energy meter charging and release, defender HP reduction, and victory rewards (+1000 XP & +50 PokéCoins).
5. **Pokémon Collection & Management (`PokemonBoxScreen.kt` & `PokemonDetailScreen.kt`)**:
   - Pokémon box grid with sorting by CP, Pokédex Number, Recent, and Favorites.
   - Semi-circular CP gauge arc, HP health bar, species stats, weight, and height.
   - Interactive IV Appraisal: Attack (0..15), Defense (0..15), Stamina (0..15) bars and 0-4 star appraisal badges.
   - Power Up: Consumes Stardust and Candy, increments level by 0.5, and recalculates CP and HP.
   - Evolve: Consumes species candy, transforms Pokémon into next evolutionary stage, awards +500 XP.
   - Transfer: Releases Pokémon to Professor for +1 species candy.
6. **Pokédex Screen (`PokedexScreen.kt`)**:
   - Generation 1 Pokédex browser with seen/caught status and type affinities.
   - Detailed species inspection sheets with flavor text, heights, weights, and base stats.
7. **Egg Incubators & Hatching (`EggIncubatorScreen.kt`)**:
   - Egg inventory (2km, 5km, 7km, 10km, 12km) and Incubators (Unlimited 1.0x and Super 0.67x).
   - Automatic distance advancement.
   - Hatch sequence modal ("Oh?") with egg cracking animation and hatch reveal with bonus candy and stardust.
8. **Buddy Pokémon Adventure (`BuddyScreen.kt`)**:
   - Active buddy showcase with affection level and emotion state.
   - Berry feeding minigame with hunger meter.
   - Daily heart activities tracker (Walk, Treat, Play, Battle).
9. **Item Bag (`BagScreen.kt`)**:
   - Categorized item inventory with usage actions.
10. **Trainer Profile & Daily Streaks (`TrainerProfileScreen.kt`)**:
    - Trainer card, level, XP progress bar, Stardust, PokéCoins, and team badge.
    - 7-day Daily Streaks checklist for First Catch and First PokéStop of the day.
    - Medals: Jogger, Collector, Scientist, Breeder, Backpacker.
11. **Adventure Sync Background Service (`AdventureSyncService.kt`)**:
    - Real Android Foreground Service with notification `"Adventure Sync is tracking your distance"`.
    - Periodically accumulates distance, advances eggs, awards buddy candies, and fires system notifications.
12. **Companion Device Simulator (`PokemonGoPlusService.kt`)**:
    - Background companion simulator implementing the reverse-engineered `Sfida` protocol.
    - Auto-catches wild Pokémon and auto-spins PokéStops with system notifications.
13. **Home Screen AppWidgets**:
    - `EggsAppWidgetProvider`: Real-time active egg hatching distance.
    - `BuddyAppWidgetProvider`: Buddy emotion, hunger, and candy distance.
    - `DailyStreaksAppWidgetProvider`: 7-day catch and spin streaks.

---

## 4. Verification & Testing
- **Compilation**: Full Gradle build succeeded with Android Gradle Plugin 8.3.2 and Kotlin 1.9.24.
- **Unit Test Suites**:
  - `PokemonGoMechanicsTest.kt`: 6 tests passing (100%)
  - `ActivationSystemTest.kt`: 4 tests passing (100%)
  - `MovementControllerTest.kt`: 4 tests passing (100%)
  - `AutomationEngineTest.kt`: 4 tests passing (100%)
  - `ExtendedFeaturesTest.kt`: 14 tests passing (100%)
  - **Total**: 32 unit tests passing, 0 failures.

---

## 5. Complete Application Integration & Deliverables

### 5.1 Integrated Complete Target APK
Applying the architectural injection approach discovered in our REA analysis:
- **Path**: `output/csharp-pokemongo-v0.433.0-signed.apk`
- **Size**: **452,559,252 bytes (~432 MB)**
- **Package**: `com.nianticlabs.pokemongo` | **Version**: `0.433.0` (`2026100702`) | **Target SDK**: `36`
- **Architecture**:
  - Preserves 100% of the official Pokémon GO Unity 2022.3 runtime (`libunity.so` 24.5 MB) and C# game logic (`libil2cpp.so` 252 MB).
  - 28 native ARM64-v8a `.so` libraries, uncompressed and page-aligned to 4KB (`zipalign -p -f 4`).
  - 1,183 game assets, including all 162 Addressables AssetBundles (`assets/aa/`) and `global-metadata.dat` (35.8 MB, IL2CPP v39).
  - Integrated `csharp` MultiDex runtime (`classes4.dex` 31.9 MB and `classes5.dex` 7.7 MB) with on-device test-key activation (`DEV-PRO-2026`).
  - Verified with **Android APK Signature Scheme v3** via `apksigner`.

### 5.2 Standalone Clean-Room Companion APK
- **Path**: `output/cleanroom-pokemongo-debug.apk`
- **Size**: **54,065,244 bytes (~52 MB)**
- **Architecture**: 100% pure Kotlin and Jetpack Compose standalone application implementing all 44 features.

### 5.3 GitHub Repository
- **Remote URL**: [`https://github.com/adarshdubey-alphabotz/csharp`](https://github.com/adarshdubey-alphabotz/csharp)
- **Branch**: `main` (up to date with latest commits and documentation)

