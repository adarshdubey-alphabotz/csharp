# csharp

An independent, native Android clean-room application and companion simulator built with modern Kotlin and Jetpack Compose. Engineered based on deep static analysis and reverse engineering findings of mobile location-based gaming architectures.

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

### 4. Key Activation & Licensing
- Testable, decoupled activation manager (`ActivationManager.kt`) utilizing local validation without external network dependencies.
- Persistent state management across app launches via `SharedPreferences`.
- Supported Local Dev Test Keys:
  - `DEV-PRO-2026`: 1-Year Pro access
  - `TRIAL-7DAYS`: 7-Day Trial access
  - `LIFETIME-PRO`: Lifetime unlocked access
  - `PRO-XXXX-XXXX`: 30-Day Pro access

### 5. Core Game Mechanics & Services
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
Comprehensive research and reverse engineering reports are located in the [`docs/`](docs/) directory:
- [`docs/architecture_report.md`](docs/architecture_report.md): Technical architecture report
- [`docs/feature_matrix.md`](docs/feature_matrix.md): Target feature matrix and test status
- [`docs/component_inventory.md`](docs/component_inventory.md): Android manifest and components inventory
- [`docs/audit_report.md`](docs/audit_report.md): Implementation audit report
