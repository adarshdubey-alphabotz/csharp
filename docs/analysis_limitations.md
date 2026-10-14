# Static Analysis Limitations & Security Boundaries

## 1. Safety & Execution Boundaries
- **No Direct Binary Execution**: In accordance with the security policy, neither the downloaded `test.apk` nor any native binaries (`.so` shared objects extracted from `lib/arm64-v8a/`) were executed directly on the host VPS.
- **No Credential Harvesting or License Circumvention**: No authentication tokens, client secrets, proprietary encryption keys, or licensing controls were accessed, bypassed, or incorporated into reconstructed source code.

## 2. Technical Limitations in Static Analysis

### 2.1 IL2CPP Native Binary Stripping
- The primary game logic resides within `lib/arm64-v8a/libil2cpp.so` (252.4 MB) which was compiled ahead-of-time (AOT) from C# bytecode to ARM64 machine code using Unity's IL2CPP compiler.
- `global-metadata.dat` (35 MB, metadata version 39) contains string literals, method tokens, and type indices, but function implementations inside `libil2cpp.so` are stripped of symbol tables.
- While string analysis and class definitions are recoverable, fine-grained control flow graph (CFG) analysis of private Unity gameplay scripts is subject to static inference rather than direct source recovery.

### 2.2 Proprietary Network RPC Protocols
- Communication with Niantic's game servers is handled inside `libholoholo.so` and `libnia-advsync-reporter-android.so` via encrypted Protocol Buffers over HTTPS/gRPC.
- The encryption involves custom client-side request signatures (incorporating device telemetry, location hashes, and anti-bot challenges).
- Consequently, legitimate re-implementation focuses on local game mechanics, domain models, local persistence, Android platform integrations, and UI state machines rather than communicating with production Niantic backend servers.

### 2.3 Obfuscation in DEX Bytecode
- Third-party SDKs (Google Play Services, Firebase, Facebook SDK, AppsFlyer) and internal analytics wrappers utilize ProGuard/R8 identifier minification (e.g., single-letter classes `a.b.c`, synthetic lambda references).
- JADX was able to successfully decompile 98.3% (7,917 out of 8,057) of classes without syntax anomalies. 140 heavily obfuscated inner methods required bytecode inspection.
- All first-party Niantic platform integration code (`com.nianticlabs.pokemongoplus`, `com.nianticlabs.platform.niaadvsynclocation`, `com.nianticproject.holoholo.libholoholo.appwidget`) was fully unobfuscated and preserved meaningful method and variable naming.
