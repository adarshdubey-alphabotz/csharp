# Technical Feasibility Assessment: Rebranding & Custom Licensing of Reference APK (`original/test.apk`)

**Target Reference Artifact**: `original/test.apk`  
**Base Package**: `com.nianticlabs.pokemongo` (Pokémon GO v0.433.0)  
**Injected Mod**: PGSharp 64-bit (`libmain.so`, 37.2 MB native ARM64 binary)  

---

## 1. Executive Summary: Is This Technically Possible with Available Artifacts?

**Verdict: PARTIALLY FEASIBLE at the outer Android OS layer; FUNDAMENTALLY BLOCKED at the in-game mod and server-authentication layers.**

Using only the supplied `original/test.apk` and standard APK reverse-engineering tools (`apktool`, `zipalign`, `apksigner`):

| Layer | Proposed Modification | Technical Reality with Available Artifacts | Feasibility |
|---|---|---|:---:|
| **Android OS Layer** | Rename app to `csharp`, change home screen launcher icon, inject custom pre-launch activation Activity. | Modifiable via `AndroidManifest.xml` and `res/` resources using `apktool`. | **CONFIRMED FEASIBLE** |
| **Repackaging & Signing** | Rebuild and sign the modified APK with a custom keystore. | Feasible using `apktool b` and `apksigner`. | **CONFIRMED FEASIBLE** |
| **In-Game PGSharp Branding** | Replace in-game "PGSharp" logo, floating menu header, and settings text with `csharp`. | **Hardcoded in native machine code** inside `libmain.so` (37.2 MB). Not accessible via Android resources or DEX code. | **BLOCKED / HIGH RISK** |
| **Mod Feature Licensing** | Unlock the in-game mod features using our `DEV-PRO-2026` test key. | `libmain.so` does not read Android `SharedPreferences`. Its feature gates are internal C++ memory flags tied to its own HTTPS server. | **BLOCKED** |
| **Live Game Authentication** | Run the original Pokémon GO game against Niantic servers. | Re-signing the APK invalidates Google Play signature. **Google Play Integrity fails**, causing Niantic's servers to reject login. | **BLOCKED (Non-Root)** |

---

## 2. Can the Branding and Activation Flow Be Changed Without Rebuilding the Entire Pokémon GO Game?

### A. Android OS-Level Branding: **YES**
The application label displayed on the Android launcher, package manager, and app info screen (`android:label`), as well as the app icon (`@mipmap/ic_launcher`), are defined in:
- `AndroidManifest.xml`
- `resources.arsc`
- `res/values/strings.xml`

These can be modified using `apktool d` without touching Unity, C# code, or `libil2cpp.so`.

### B. In-Game Mod Overlay Branding: **NO**
The floating star icon, the overlay drawer, the settings menu, and the in-game branding ("PGSharp") are **not part of the Android resource system or Unity UGUI**. 
They are drawn directly into the native graphics pipeline (OpenGL ES / Vulkan) via an injected ImGui/custom renderer compiled into **`libmain.so` (37.2 MB)**.
- There are **no string entries** in `res/values/strings.xml` for PGSharp.
- There is **no Java/Kotlin code** in the DEX files for PGSharp.
- All text strings and visual assets for the mod menu are compiled into stripped native ARM64 machine code. Changing them without the original C++ source requires binary disassembly and hex-patching.

---

## 3. Can Our Test-Key System Be Integrated While Preserving the Original Gameplay?

### A. Pre-Launch Gate (Android Level): **YES**
You can inject a custom Android Activity (e.g. `ActivationActivity`) into `AndroidManifest.xml` that acts as the `MAIN/LAUNCHER` entry point before `PokemonGoActivity`:
1. The app opens to our custom activation screen.
2. The user enters `DEV-PRO-2026`.
3. The key is validated locally against our `LocalDevActivationValidator` and saved in `SharedPreferences`.
4. Upon successful validation, it launches `com.nianticlabs.pokemongo.PokemonGoActivity`.

### B. Communication with the In-Game Mod Layer: **NO**
This pre-launch screen will **not unlock PGSharp's in-game mod features**.
- The mod logic running inside `libmain.so` does not poll Android `SharedPreferences`.
- It maintains its own internal licensing state and expects to communicate with `https://www.pgsharp.com` over an encrypted channel.
- Unless `libmain.so` is binary-patched to bypass its internal license checks or hook into our shared preferences via JNI, entering a key in an external screen will leave the in-game mod features locked.

---

## 4. Can the PGSharp Layer Be Retained While Replacing Its Branding?

**Verdict: HIGHLY UNCERTAIN and structurally fragile.**

PGSharp's functionality and its branding are part of the **exact same monolithic compiled binary**: `libmain.so` (37,258,240 bytes).
- The binary is stripped (only `JNI_OnLoad` is exported).
- The strings inside `libmain.so` are obfuscated/encrypted.
- Native binaries commonly incorporate internal checksumming or length validation. Modifying strings in-place via binary hex-editing can corrupt memory alignments or trigger internal crashes (`SIGSEGV`).
- Because PGSharp is closed-source proprietary software, there is no clean separation between its hooking logic (joystick, throw override, coordinate injection) and its visual presentation.

---

## 5. What Specific Technical Obstacles Could Prevent This?

1. **Google Play Integrity & SafetyNet Attestation**:
   - The reference APK is signed by the original distributor.
   - Any modification (even editing a single string in `AndroidManifest.xml`) requires re-signing the APK with a new keystore.
   - Niantic's servers verify the app's cryptographic signature via Google Play Integrity API during the login handshake (`AuthenticateGoogleRequestProto`).
   - On a standard (non-rooted) Android device, an APK signed with an unauthorized certificate **will fail Play Integrity**, and the server will return: *"Unable to authenticate. Please try again."*
2. **Native Code Obfuscation in `libmain.so`**:
   - 37.2 MB of compiled ARM64 assembly with no symbols.
   - Decompilation via Ghidra yields low-level C pseudocode with lost variable names and inlined control flow.
3. **Internal Licensing Decoupling**:
   - The feature gates for joystick, teleport, enhanced throw, and auto-catch are located inside `libmain.so`.
   - Without binary-level hooking of `libmain.so`, an external Java/Kotlin test-key activation screen cannot communicate with or unlock these native features.
4. **Anti-Tamper & Packaging Checks**:
   - Modded client loaders frequently perform self-integrity checks (comparing APK hashes or DEX CRC32 checksums) to detect third-party repackaging.

---

## 6. Feasibility Breakdown by Component

| Component / Task | Feasibility Status | Requirement to Achieve |
|---|:---:|---|
| Change OS app title to `csharp` | **CONFIRMED FEASIBLE** | Edit `strings.xml` / `AndroidManifest.xml` via `apktool`. |
| Change app launcher icon | **CONFIRMED FEASIBLE** | Replace PNG files in `res/mipmap-*/` via `apktool`. |
| Add custom Android activation UI | **CONFIRMED FEASIBLE** | Add compiled DEX classes and declare Activity in `AndroidManifest.xml`. |
| Rebuild and sign installable APK | **CONFIRMED FEASIBLE** | Use standard `apktool b` + `apksigner`. |
| Replace in-game PGSharp branding / menu | **UNCERTAIN / HIGH RISK** | Requires reverse-engineering and binary-patching `libmain.so`. |
| Unlock mod features with our test key | **BLOCKED WITH CURRENT ARTIFACTS** | Requires binary patch or memory hook into `libmain.so`'s licensing routine. |
| Connect to official Niantic servers after re-signing | **BLOCKED ON STOCK ANDROID** | Requires root-level Play Integrity spoofing (e.g. Magisk + PlayIntegrityFix). |

---

## 7. Recommended Technical Approach

If your primary objective is to **preserve the authentic Pokémon GO game (Unity engine, 3D graphics, live Niantic servers)** rather than a standalone simulation, attempting to reverse-engineer and patch the closed-source, obfuscated 37MB PGSharp binary is the highest-friction and most brittle pathway.

### The Standard Industry Architecture:
Instead of modifying a pre-modded binary (`test.apk`), modern Android reverse engineers achieve this via **clean native hook injection**:

1. **Start with the official unmodified APK**: Retains full game assets, Unity 2022.3 engine, and proper binary alignments.
2. **Develop the `csharp` mod layer independently**:
   - Write a lightweight, dedicated native injection library (`.so`) using open-source hooking (such as Dobby or And64InlineHook).
   - Hook the exact target functions identified in our analysis (`NianticLocationProvider` for coordinates, throw evaluation in `libil2cpp.so` for Enhanced Throw).
   - Draw our own overlay with 100% `csharp` branding and our own test-key licensing validator (`DEV-PRO-2026`).
3. **Benefit**: You completely control the branding, the UI, the activation logic, and the code, while the underlying Pokémon GO game engine runs completely intact.
