# Comprehensive Architecture & Integration Report: Pokémon GO & `csharp`

**Artifacts Analyzed & Integrated**:
- **Official Pokémon GO Client**: v0.431.0 (`base.apk` + `split_config.arm64_v8a.apk`)
- **PGSharp Modified Reference Client**: v0.433.0 (`original/test.apk`, SHA-256: `80aa97f7ebdf04ecd972b82a2a4495f087331024487d77ed1b1c3c457e547315`)
- **Integrated Target Application**: `output/csharp-pokemongo-v0.433.0-signed.apk` (452 MB)
- **Source Codebase**: [`csharp` Repository](https://github.com/AkaneSakuramori/csharp)

---

## 1. Executive Summary

This report establishes the forensic architecture of the official Pokémon GO mobile client, the exact binary injection methodology utilized by PGSharp, and how our `csharp` implementation integrates into the complete application while preserving the original Unity 2022.3 game engine, 3D assets, audio pipelines, and existing gameplay functionality.

```
+--------------------------------------------------------------------------------------------------------+
|                                  csharp Integrated Application Architecture                            |
|                                                                                                        |
|  +---------------------------------------------------+  +-------------------------------------------+  |
|  |             Android Platform & DEX Layer          |  |         csharp Subsystem (classes4/5)     |  |
|  |  +---------------------------------------------+  |  |  +-------------------------------------+  |  |
|  |  | UnityMainActivity / WorkManagerApplication  |  |  |  | ActivationManager (Test-Key Validation|  |  |
|  |  | LocationForegroundService (Adventure Sync)  |  |  |  | MovementController (Spoofing/Joystick|  |  |
|  |  | AppWidgets (Eggs/Buddy/Streaks)              |  |  |  | AutomationEngine (Auto Catch/Spin/IV)|  |  |
|  |  +---------------------------------------------+  |  |  | Jetpack Compose Window Decor Overlay|  |  |
|  +-------------------------+-------------------------+  +---------------------+---------------------+  |
|                            |                                                  |                        |
|                            | JNI / Dalvik ART MultiDex ClassLoader            |                        |
|  +-------------------------v--------------------------------------------------v---------------------+  |
|  |                                  Native Engine Layer (lib/arm64-v8a/)                            |  |
|  |                                                                                                  |  |
|  |  +----------------------+  +-----------------------+  +--------------------+  +---------------+  |  |
|  |  | Unity Engine 2022.3  |  | AOT C# Game Logic     |  | Niantic Security   |  | FMOD Audio    |  |  |
|  |  | (libunity.so 24.5MB) |  | (libil2cpp.so 252MB)  |  | (libholoholo.so)   |  | (libfmod.so)  |  |  |
|  |  +----------------------+  +-----------------------+  +--------------------+  +---------------+  |  |
|  |  +----------------------+  +-----------------------+  +--------------------+  +---------------+  |  |
|  |  | Google ARCore        |  | AdventureSync Native  |  | Swappy Frame Pacing|  | Platform JNI  |  |  |
|  |  | (libUnityARCore.so)  |  | (libadventuresync.so) |  | (libswappywrapper) |  | (libNiantic..) |  |  |
|  |  +----------------------+  +-----------------------+  +--------------------+  +---------------+  |  |
|  +--------------------------------------------------------------------------------------------------+  |
|                                                                                                        |
|  +--------------------------------------------------------------------------------------------------+  |
|  |                                 Assets & Resources Layer                                         |  |
|  |  - assets/aa/Android/ (162 Addressables AssetBundles: Shaders, Biomes, UI)                       |  |
|  |  - assets/bin/Data/Managed/Metadata/global-metadata.dat (35.8 MB, IL2CPP v39, 400,840 tokens)    |  |
|  |  - assets/text/ (i18n localization catalogs: en, de, fr, es, it, zh)                             |  |
|  |  - res/ and resources.arsc (134 language tables, layouts, drawable assets)                       |  |
|  +--------------------------------------------------------------------------------------------------+  |
+--------------------------------------------------------------------------------------------------------+
```

---

## 2. Forensic Discovery: How PGSharp Operates

Through differential binary analysis of `original/test.apk` against `analysis/official_base/`, the exact mechanisms used by PGSharp were discovered:

### 2.1 The Entrypoint Hijack (`libmain.so`)
- In standard Unity Android games, `com.unity3d.player.UnityPlayerActivity` loads `libmain.so` at startup:
  ```java
  static {
      System.loadLibrary("main");
  }
  ```
- The official `libmain.so` is a minimal 6.6 KB stub that simply initializes the Unity engine.
- PGSharp **replaces** `libmain.so` with its own 38.3 MB monolithic binary (`SONAME: libhela.so`).
- When Android calls `JNI_OnLoad` in `libmain.so`, PGSharp gains execution control **before** Unity or IL2CPP even starts.

### 2.2 Linker & PLT Interception
- PGSharp hooks the dynamic linker (`libdl.so`) functions: `dlopen` and `dlsym`.
- When `libmain.so` proceeds to load `libunity.so` and `libil2cpp.so`, PGSharp intercepts the loaded ELF handles and installs hooks on internal game functions.

### 2.3 Direct Graphics Pipeline Hooking
- Inspecting `DT_NEEDED` on `libmain.so` revealed direct linkages to:
  - `libEGL.so`
  - `libGLESv2.so`
  - `libjnigraphics.so`
  - `libandroid.so`
- PGSharp installs a hook on `eglSwapBuffers`. At the end of every rendered frame, PGSharp draws its floating joystick, HUD banners, and ImGui configuration windows directly into the OpenGL ES framebuffer over the Unity game scene.

### 2.4 DEX Integrity
- **Zero Java changes**: PGSharp left all 23,365 classes in `classes.dex`, `classes2.dex`, and `classes3.dex` untouched. This minimized tampering flags detectable by simple Java classpath or dex checksum scanners.

---

## 3. The `csharp` Complete Application Integration

Rather than relying on closed-source, obfuscated native code (`libmain.so` with O-LLVM), `csharp` achieves the same end goal through an elegant, robust Android architecture:

### 3.1 MultiDex Runtime Integration
- The official game utilizes 3 DEX files (`classes.dex`, `classes2.dex`, `classes3.dex`).
- Android ART natively scans and loads any additional DEX files matching `classesN.dex`.
- `csharp` packages its complete Kotlin/Compose subsystem into `classes4.dex` (31.9 MB) and `classes5.dex` (7.7 MB).
- ART automatically initializes these classes into the application's root `PathClassLoader`.

### 3.2 Overlay Presentation Architecture
- Rather than hooking low-level `eglSwapBuffers` (which is fragile across GPU chipsets and Vulkan/GLES transitions), `csharp` attaches a hardware-accelerated `ComposeView` directly to the `Activity.window.decorView`.
- This ensures:
  - 60 FPS smooth animations using Android's Choreographer.
  - Native gesture recognition (drag, tap, long-press for stealth mode).
  - Clean separation between Unity's 3D render thread and the overlay UI thread.

### 3.3 Movement & Location Interception
- The official game processes GPS locations through `com.nianticlabs.nia.location.NianticLocationManager.notifyLocationUpdate(providerName, location, status)`.
- `csharp` feeds spoofed coordinates from `MovementController` directly into the location pipeline:
  - Supports continuous joystick velocity stepping.
  - Supports tap-to-walk waypoint navigation.
  - Supports double-tap instant teleportation.
  - Applies the exact Niantic distance-to-cooldown softban curve (up to 7200 seconds).

### 3.4 Key Activation & Licensing
- PGSharp relies on external proprietary servers for license checking.
- `csharp` integrates an on-device `ActivationManager`:
  - Supports standard test keys: `DEV-PRO-2026`, `TRIAL-7DAYS`, `LIFETIME-PRO`, and `PRO-XXXX-XXXX`.
  - Normalizes format and persists state in `SharedPreferences`.
  - Dynamically gates 24 Pro features (`ProFeature` enum) without requiring external network connectivity.

---

## 4. Verification & Packaging Summary

The complete application was assembled into `output/csharp-pokemongo-v0.433.0-signed.apk`:
- **Package Name**: `com.nianticlabs.pokemongo`
- **Version**: `0.433.0` (`2026100702`)
- **APK Size**: 452,559,252 bytes (~432 MB)
- **Native Libraries**: 28 libraries in `lib/arm64-v8a/`, uncompressed and page-aligned to 4096 bytes (`zipalign -p -f 4`).
- **DEX Files**: 5 files (`classes.dex` through `classes5.dex`).
- **Assets**: 1,183 files, including all 162 Addressables AssetBundles and `global-metadata.dat`.
- **Signature**: Verified with Android APK Signature Scheme v3 via `apksigner`.
