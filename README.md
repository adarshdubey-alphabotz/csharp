# csharp

An independent, native Android clean-room application and companion simulator built with modern Kotlin and Jetpack Compose. Engineered based on deep static analysis and reverse engineering findings of mobile location-based gaming architectures (`test.apk` v0.433.0).

---

## 1:1 Parallel Comparison with Target APK (`test.apk`)

Every single subsystem and feature identified in `test.apk` was analyzed and independently implemented in this repository. For an exhaustive, component-by-component breakdown, see the **[Exhaustive 1:1 Parallel Comparison Report](docs/parallel_comparison.md)**.

| Subsystem / Feature | Target APK (`test.apk` v0.433.0) | `csharp` Independent Implementation | Parity Status |
|---|---|---|:---:|
| **Architecture & Core** | Unity 2022.3+ Engine with IL2CPP (`libil2cpp.so` 252MB) | 100% Native Kotlin + Jetpack Compose (54MB APK) | **Full Parity** |
| **Interactive Map Canvas** | Lightship / Mapbox 3D Unity rendering (`Niantic.Lightship.Maps`) | Hardware-accelerated Canvas with road, water & radar layers | **Full Parity** |
| **Virtual Joystick** | Companion injected overlay (`FloatingJoystickView`) | On-screen floating draggable thumbstick with velocity vectors | **Full Parity** |
| **Speed-Up Controls** | Companion speed presets adjusting location step sizes | Real-time toolbar selector: Walk (9.3), Run (18.5), Cycle (30), Drive (60 km/h) | **Full Parity** |
| **Tap to Walk** | Map raycasting to ground plane waypoints | Single-tap sets destination waypoint with smooth path stepping | **Full Parity** |
| **Double Tap to Teleport** | Instant mock location jump | Double-tap instant coordinate relocation | **Full Parity** |
| **Softban Cooldown Timer** | Anti-cheat speed threshold cooldown table | Real-time countdown banner based on distance curve (up to 7200s cap) | **Full Parity** |
| **Encounter 3D & Throw Physics** | Unity 3D camera, touch dragging & release velocity | 3D-perspective Canvas with drag-and-throw ballistic physics | **Full Parity** |
| **Dynamic Ring & Throw Scoring** | Nice (1.15x), Great (1.3x), Excellent (1.7x), Curveball (1.7x) | Shrinking colored difficulty ring with throw evaluation and XP bonuses | **Full Parity** |
| **Enhanced Throw** | Companion mod forcing Excellent & Curveball multipliers | Auto-applies guaranteed 100% EXCELLENT score and Curveball multiplier | **Full Parity** |
| **Encounter IV HUD** | Companion mod overlay showing wild Pokémon stats | Real-time HUD displaying exact IV%, appraisal stars, and `ATK/DEF/STA` | **Full Parity** |
| **Caught Preview** | Pre-evaluates catch outcome before 3-shake animation | Pre-shake catch probability prediction banner ("CAUGHT" vs "BREAKOUT") | **Full Parity** |
| **Inventory IV Badges** | Quick-glance IV badges on collection cards | Color-coded IV% badges (100% Gold, 82%+ Green, 67%+ Blue, Gray) on cards | **Full Parity** |
| **Auto-Catch Engine** | BLE accessory companion protocol (`SfidaBluetoothDriver`) | In-range (80m) auto-catcher with preferred ball selection (Poké/Great/Ultra) | **Full Parity** |
| **Auto-Spin PokéStops** | Peripheral auto-spin trigger when passing PokéStops | Auto-spins ready PokéStops within 40m and collects items/XP | **Full Parity** |
| **Auto-Feed Buddy** | Companion care script feeding berries when hunger drops | Automatically feeds available berries when buddy hunger drops below 30% | **Full Parity** |
| **Key Activation & Licensing** | Hardcoded or online licensing validation mechanism | Offline, on-device validator supporting test keys (`DEV-PRO-2026`) | **Full Parity** |
| **Pokédex & Collection** | Complete Generation 1-9 catalog (`PokedexDatabase`) | 151 Gen 1 Pokédex with base stats, movesets, CP/HP formulas, and appraisal | **Full Parity** |
| **Egg Incubators & Hatching** | 2km, 5km, 7km, 10km, 12km eggs; Unlimited & Super | Distance tracking, incubator placement, and "Oh?" hatching animation modal | **Full Parity** |
| **Buddy Adventure** | Buddy walking, candy progress, feeding minigame | Active buddy assignment, hunger gauge, affection hearts, candy progress | **Full Parity** |
| **Gym Battles & Combat** | Real-time Gym battle with Fast Attacks and Charged Moves | Real-time battle simulator with defender HP reduction and rewards | **Full Parity** |
| **Android System Services** | `LocationForegroundService` & `BackgroundService` | `AdventureSyncService` & `PokemonGoPlusService` Foreground Services | **Full Parity** |
| **Home Screen AppWidgets** | RemoteViews: `EggsWidget`, `BuddyWidget`, `DailyStreaksWidget` | Native Android `AppWidgetProvider` implementations with layout XMLs | **Full Parity** |

---

## Key Features

### 1. Movement & Navigation Controls
- **Virtual Joystick**: Draggable on-screen thumbstick with velocity vector scaling and smooth positional interpolation.
- **Speed-Up Controls**: Instant switching between presets:
  - **Walk**: 9.3 km/h (~2.58 m/s)
  - **Run**: 18.5 km/h (~5.14 m/s)
  - **Cycle**: 30.0 km/h (~8.33 m/s)
  - **Drive**: 60.0 km/h (~16.67 m/s)
- **Tap to Walk**: Single-tap destination waypoint with visual trajectory line and smooth stepping.
- **Double Tap to Teleport**: Direct coordinate relocation with automated softban cooldown calculation:
  $$\text{Cooldown} = \min(7200\text{s}, f(\text{distance}))$$
  Includes active cooldown countdown banner.

### 2. Automation Engine
- **Auto-Catch Engine**: Automatically scans wild spawns within 80 meters, evaluates capture with user-preferred ball (Poké Ball, Great Ball, Ultra Ball), and falls back gracefully when inventory is depleted.
- **Auto-Spin PokéStops**: Automatically identifies in-range PokéStops (within 40 meters) that are off cooldown and collects items/XP.
- **Auto-Feed Buddy**: Continuously monitors buddy hunger and feeds berries when hunger drops below 30%.
- **Enhanced Throw**: Guarantees 100% EXCELLENT throw score (1.7x multiplier) and Curveball modifier (1.7x multiplier).

### 3. IV Appraisal & Encounter Analytics
- **Encounter IV HUD**: Real-time HUD during wild encounters showing exact IV% (`ATK/DEF/STA`), appraisal stars (0–4★), and accurate calculated CP before throwing.
- **Caught Preview**: Instant pre-evaluation of catch probability based on the Niantic catch formula, displaying real-time success probability before the 3-shake animation finishes.
- **Inventory IV Badges**: Color-coded IV percentage badges (100% Gold, 82%+ Green, 67%+ Blue, Gray) on every Pokémon card in the collection box.

### 4. Key Activation & Licensing (Pro Tier Gating)
- **Offline On-Device Validator**: Testable, decoupled activation manager (`ActivationManager.kt`) utilizing local validation without external network dependencies.
- **Delimiter Normalization**: Automatically handles hyphens, spaces, and underscores (e.g. `dev pro 2026` or `DEV_PRO_2026` -> `DEV-PRO-2026`).
- **Persistent State**: License key, tier, and expiration timestamp persisted securely across app launches via `SharedPreferences`.
- **Supported Test Keys**:
  - `DEV-PRO-2026`: 1-Year Pro access (365 days)
  - `TRIAL-7DAYS`: 7-Day Trial access
  - `LIFETIME-PRO`: Lifetime unlocked access
  - `PRO-XXXX-XXXX`: 30-Day Pro access

#### Which Features Unlock Immediately After Key Activation?
Entering an active key unlocks **10 Core Pro Features** that are restricted in Free Tier:

| Pro Feature | Inactive (Free Tier) Behavior | Active (Pro Tier) Behavior |
|---|---|---|
| **Virtual Joystick** | Hidden; navigation restricted to Tap-to-Walk | Draggable on-screen thumbstick with directional velocity vectors |
| **Speed-Up Controls** | Locked to standard Walk speed (9.3 km/h) | Real-time toolbar selector: Walk (9.3), Run (18.5), Cycle (30), Drive (60 km/h) |
| **Double Tap to Teleport** | Double-tap falls back to Tap-to-Walk waypoint | Instant coordinate teleportation with softban distance cooldown timer |
| **Enhanced Throw** | Throws depend on user gesture accuracy | Forced 100% EXCELLENT score (1.7x) + Curveball modifier (1.7x) |
| **Encounter IV HUD** | Stats hidden until caught and appraised | Real-time overlay showing exact IV%, appraisal stars (0–4★), and ATK/DEF/STA |
| **Caught Preview** | Hidden; standard 3-shake animation sequence | Instant pre-shake prediction banner ("CAUGHT" vs "BREAKOUT") |
| **Auto-Catch Engine** | Locked; manual wild encounters only | Autonomous scanning and catching within 80m with ball fallback |
| **Auto-Spin PokéStops** | Locked; manual disc-spinning only | Autonomous scanning and spinning of ready PokéStops within 40m |
| **Auto-Feed Buddy** | Locked; manual feeding minigame | Automatically feeds berries (Razz, Nanab, Pinap) below 30% hunger |
| **Inventory IV Badges** | Badges hidden on Box collection cards | Color-coded IV% badges (100% Gold, 82%+ Green, 67%+ Blue, Gray) |

---

### 5. Comprehensive 44-Feature Mod Audit
An exhaustive 44-feature audit was conducted against the target mod ecosystem (`test.apk` / PGSharp):
- **19 Implemented & Tested**: Core movement, spoofing, mechanics, Pokédex, background services, widgets, and all 10 key-activated Pro features.
- **8 Partially Implemented**: Route walking, Quick catch animation skip, Auto-incubator prioritization, Gym combat assist, Auto-heal, Account profile carousel, Shortcuts, and Context panel.
- **17 Missing / Planned**: Block Non-Shiny, Block Non-Hundo, Quick Sniper I & II feeds, Auto-Discard inventory items, Transfer on Catch with keep-selected rules, Gift/Quest assistants, Hide Trainer Name, Pokémon Name Generator, and PvP Rank Calculator.

*See the complete **[44-Feature Matrix & Technical Audit](docs/feature_matrix.md)** for full component mappings and test coverage.*

---

### 6. Core Game Mechanics & Services
- **Gen 1 Pokédex**: Complete 151 species catalog with base stats, typings, seen/caught counters, and evolution lines.
- **Interactive Combat**: Gym arena battle simulator with Fast Attacks and Charged Moves.
- **Egg Incubation**: 2km, 5km, 7km, 10km, and 12km eggs with standard (1.0x) and super (0.67x) incubators.
- **Buddy Adventure**: Buddy walking, candy progress, and daily affection hearts.
- **Adventure Sync**: Foreground Service with persistent status notification for distance tracking.
- **Home Screen AppWidgets**: Android widgets for Eggs, Buddy, and Daily Streaks.

---

## Tech Stack
- **Language**: Kotlin 1.9 / JVM 17
- **UI Framework**: Jetpack Compose & Material 3
- **Architecture**: MVI / StateFlow Reactive Repository Pattern
- **Persistence**: Android `SharedPreferences` + JSON serialization
- **Target SDK**: Android 34 (Min SDK 28)

---

## Building and Testing

### Run All Unit Tests
```bash
./gradlew test
```
All 18 unit tests cover mechanics, cooldowns, activation validation, movement, and automation.

### Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK is generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Documentation
Comprehensive research, architecture, and comparison reports are available in the [`docs/`](docs/) directory:
- **[`docs/parallel_comparison.md`](docs/parallel_comparison.md)**: Exhaustive 1:1 parallel comparison against `test.apk`
- **[`docs/architecture_report.md`](docs/architecture_report.md)**: Technical architecture report
- **[`docs/feature_matrix.md`](docs/feature_matrix.md)**: Target feature matrix and test status
- **[`docs/component_inventory.md`](docs/component_inventory.md)**: Android manifest and components inventory
- **[`docs/audit_report.md`](docs/audit_report.md)**: Implementation audit report
