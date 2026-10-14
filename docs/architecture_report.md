# Architecture Report: Pokémon GO Android Application

## 1. Executive Summary
- **Target Application**: Pokémon GO
- **Package Name**: `com.nianticlabs.pokemongo`
- **Application Class**: `com.nianticproject.holoholo.libholoholo.unity.WorkManagerApplication`
- **Main Launch Activity**: `com.nianticproject.holoholo.libholoholo.unity.UnityMainActivity`
- **Target SDK**: 36 (Android 16 / Vanilla Ice Cream preview target)
- **Min SDK**: 28 (Android 9.0 Pie)
- **Version Name / Code**: `0.433.0` / `2026100702`
- **APK SHA-256**: `80aa97f7ebdf04ecd972b82a2a4495f087331024487d77ed1b1c3c457e547315`
- **File Size**: 210,412,021 bytes (~200.67 MB)

## 2. System Architecture Overview
The application uses a hybrid dual-layer architecture combining:
1. **Native Unity/C++ Core Engine Layer**:
   - Unity Game Engine runtime (`libunity.so`, version 2022.3+)
   - AOT Compiled C# Game Logic via IL2CPP (`libil2cpp.so`, 252 MB) and Metadata (`global-metadata.dat`, v39, 35 MB)
   - Custom Niantic Native Shared Objects:
     - `libholoholo.so` (13.3 MB): Core game client logic, protobuf serialization, and network RPC transport
     - `libadventuresync.so` (4.2 MB) & `libnia-advsync-reporter-android.so` (5.3 MB): Background tracking and metrics
     - `libNianticLabsPlugin.so` (13.0 MB): Native bridge connecting Unity to Android platform services
     - `libswappywrapper.so`: Android Frame Pacing library
     - `libfmod.so` & `libfmodstudio.so`: FMOD low-level and studio audio engine
     - `libarcore_sdk_c.so` & `libUnityARCore.so`: Google ARCore augmented reality subsystem
2. **Android Platform / Java-Kotlin Framework Layer**:
   - Packaged in 3 Dalvik Executable (DEX) files containing ~167,500 methods and ~8,057 classes
   - Android lifecycle management via `UnityMainActivity` (extending `UnityPlayerActivity`)
   - Background Work & Geofencing via `androidx.work.WorkManager` and `LocationForegroundService`
   - Bluetooth Low Energy (BLE) peripheral driver for companion accessories (Pokémon GO Plus / Poké Ball Plus / Go Plus +) in `com.nianticlabs.pokemongoplus`
   - Android AppWidgets subsystem in `com.nianticproject.holoholo.libholoholo.appwidget` (Buddy, Eggs, Daily Streaks, Activity Summary)
   - Secure Key-Value persistence and multi-process data sharing via ContentProviders (`KeyValueContentProvider`, `LoginContentProvider`)

```
+-----------------------------------------------------------------------------------+
|                           Android Platform Layer (DEX)                            |
|                                                                                   |
|  +--------------------+  +---------------------+  +----------------------------+  |
|  | UnityMainActivity  |  | AppWidgets Provider |  | PokemonGoPlus BLE Service  |  |
|  | (Lifecycle Bridge) |  | (Buddy/Eggs/Streaks)|  | (Sfida Bluetooth Driver)   |  |
|  +---------+----------+  +----------+----------+  +-------------+--------------+  |
|            |                        |                           |                 |
|  +---------v------------------------v---------------------------v--------------+  |
|  |     Adventure Sync & Location Service (LocationForegroundService / WorkMgr) |  |
|  +----------------------------------+------------------------------------------+  |
+-------------------------------------|---------------------------------------------+
                                      | JNI / ContentProviders / SharedFiles
+-------------------------------------v---------------------------------------------+
|                           Native Engine Layer (arm64-v8a)                         |
|                                                                                   |
|  +--------------------+  +---------------------+  +----------------------------+  |
|  | Unity Engine       |  | il2cpp C# Code      |  | libholoholo.so             |  |
|  | (libunity.so)      |  | (libil2cpp.so 252MB)|  | (Game RPCs, Protobuf)      |  |
|  +--------------------+  +---------------------+  +----------------------------+  |
|  | Audio (FMOD)       |  | ARCore Subsystem    |  | AdventureSync Native       |  |
|  +--------------------+  +---------------------+  +----------------------------+  |
+-----------------------------------------------------------------------------------+
```

## 3. Execution Flows & Key Subsystems

### 3.1 Startup & Lifecycle
1. `WorkManagerApplication.onCreate()` initializes Android WorkManager background task factories and crash handlers.
2. `UnityMainActivity.onCreate()` configures edge-to-edge window insets, loads `NianticPluginLoader` dynamically by inspecting manifest `<meta-data>`, and initializes Unity runtime.
3. In `onResume()`, the activity invokes:
   - `AdvsyncPermissionsApi.startPermissionFromResume()`
   - `AdvsyncReporterApi.startReporterFromResume()`
   - `AdvsyncFitnessApi.startFitnessFromResume()`
   - `NiaAdvsyncLocationApi.startLocation()`
4. Unity initializes C# assemblies from `global-metadata.dat` via `il2cpp_init()`.

### 3.2 Adventure Sync & Background Tracking
- **LocationForegroundService**: Runs as an Android Foreground Service displaying a persistent notification `"Routes is running in the background"`.
- Uses Kotlin Coroutines (`locationUpdateCoroutineScope`, `reportLocationCoroutineScope`) and Flow to periodically collect GPS `LocationData` breadcrumbs at a default interval of 10 seconds.
- Breadcrumbs are serialized into protobufs (`UpdateBreadcrumbHistoryRequestProto`) and encrypted/logged for distance calculations.
- Fitness steps and distance are queried from Google Fit / Android Health Connect APIs (`AdvsyncFitnessApi`) and synced with egg hatching and buddy candy progress.

### 3.3 Companion Device (Pokémon GO Plus / "Sfida")
- BLE driver implemented in `SfidaBluetoothDriver`, `SfidaPeripheral`, and `BackgroundService`.
- GATT Services:
  - Device Control: `21c50462-67cb-63a3-5c4c-82b5b9939aeb`
  - LED & Haptic Vibration: `21c50462-67cb-63a3-5c4c-82b5b9939aec`
  - Button Notification: `21c50462-67cb-63a3-5c4c-82b5b9939aed`
  - Certification Challenge Service: `bbe87709-5b89-4433-ab7f-8b8eef0d8e37`
  - Battery Service: standard `0000180f-0000-1000-8000-00805f9b34fb`
- Notifies Android notification center when companion device interacts with wild Pokémon (`kCapturedPokemon`, `kPokemonEscaped`, `kOutOfPokeballs`) or spins PokéStops (`kRetrievedItems`, `kPokestopCooldown`).

### 3.4 Android AppWidgets
- Native Android RemoteViews widgets that display real-time game data on the Android home screen without launching the full 3D game:
  - `EggsWidget`: Displays up to 4 active incubators with distance bars (e.g., 3.5 / 5.0 km) and egg graphics. Deep links to `notification://dl_action=OPEN_POKEMON_INVENTORY,dl_tab=EGGS`.
  - `BuddyWidget`: Displays buddy avatar image, emotion icon, hunger bar, affection distance, and gifts. Deep links to `notification://dl_action=OPEN_BUDDY`.
  - `DailyStreaksWidget`: Displays 7-day Pokestop spin and Pokemon catch progress indicators.
  - `ActivitySummaryWidget`: Displays weekly distance progress towards 5km, 25km, 50km fitness milestones.
- Communication with game: Proto serialized payloads (`AdventureSyncProgress.AdventureSyncProgressResponse`) stored in private app storage or via `KeyValueContentProvider`.
