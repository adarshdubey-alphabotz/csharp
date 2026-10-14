# Component Inventory: Pokémon GO APK (v0.433.0)

## 1. Package & Build Specifications
- **Package Name**: `com.nianticlabs.pokemongo`
- **Application Label**: `Pokémon GO`
- **Compile SDK**: `36` (Android 16 preview)
- **Target SDK**: `36`
- **Min SDK**: `28` (Android 9.0)
- **Supported ABIs**: `arm64-v8a`

## 2. Permissions Declared & Requested
| Permission | Protection Level / Notes |
|------------|--------------------------|
| `android.permission.ACCESS_FINE_LOCATION` | Runtime: GPS location for map navigation and encounter placement |
| `android.permission.ACCESS_COARSE_LOCATION` | Runtime: Network-based location |
| `android.permission.ACCESS_BACKGROUND_LOCATION` | Runtime: Adventure Sync background distance and geofence tracking |
| `android.permission.ACTIVITY_RECOGNITION` | Runtime: Step counter and motion detection (walking/running/driving) |
| `android.permission.CAMERA` | Runtime: AR (Augmented Reality) encounter mode & Buddy AR snapshot |
| `android.permission.BLUETOOTH_SCAN` | Runtime: BLE discovery for Pokémon GO Plus / Auto-catcher (`neverForLocation` flag not set) |
| `android.permission.BLUETOOTH_CONNECT` | Runtime: BLE connection to peripheral companion devices |
| `android.permission.BLUETOOTH_ADVERTISE` | Runtime: Peripheral synchronization |
| `android.permission.FOREGROUND_SERVICE` | Normal: Enables `LocationForegroundService` and `BackgroundService` |
| `android.permission.POST_NOTIFICATIONS` | Runtime (API 33+): Push & local notifications for catches/eggs |
| `android.permission.VIBRATE` | Normal: Haptic feedback for spawns, ball shakes, and throws |
| `android.permission.INTERNET` | Normal: Network communication with Niantic RPC servers |
| `android.permission.ACCESS_NETWORK_STATE` | Normal: Connectivity checks |
| `android.permission.WAKE_LOCK` | Normal: CPU stay awake during background tracking tasks |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Normal: Resume background geofences and sensors on phone reboot |
| `com.android.vending.BILLING` | In-app purchases via Google Play Billing |
| `com.samsung.android.iap.permission.BILLING` | In-app purchases via Samsung Galaxy Store |
| `android.permission.USE_BIOMETRIC` | Biometric authentication support |

## 3. Core Android Activities
| Activity Class | Exported | Purpose |
|----------------|----------|---------|
| `com.nianticproject.holoholo.libholoholo.unity.UnityMainActivity` | **true** (Launcher) | Main entry point; hosts Unity 3D engine surface, manages lifecycle, deep links, and plugin dispatch. |
| `com.nianticlabs.background.configuration.InitialDialog` | false | Adventure Sync onboarding permission request dialog. |
| `com.nianticlabs.background.configuration.ManualPermissionDialog` | false | Dialog prompting user to enable background location in Android Settings. |
| `com.nianticlabs.background.configuration.DisableDialog` | false | Dialog confirming disablement of Adventure Sync. |
| `com.nianticlabs.platform.phoneauth.BiometricPromptActivity` | false | Biometric authentication prompt. |
| `com.google.android.gms.auth.api.signin.internal.SignInHubActivity` | false | Google Sign-in flow. |
| `com.android.billingclient.api.ProxyBillingActivity` | false | Google Play In-App Purchase modal. |
| `com.facebook.unity.FBUnityLoginActivity` | false | Facebook login integration for Unity. |

## 4. Background Services
| Service Class | Exported | Foreground / Purpose |
|---------------|----------|----------------------|
| `com.nianticlabs.platform.niaadvsynclocation.background.LocationForegroundService` | false | **Foreground Service**: Tracks real-time GPS breadcrumbs for the Routes feature and geofencing. Displays persistent notification `"Routes is running in the background"`. |
| `com.nianticlabs.pokemongoplus.service.BackgroundService` | false | **Foreground Service**: Manages BLE connection to Pokémon GO Plus accessory, dispatches button clicks, and flashes LED/vibrations. |
| `com.nianticlabs.background.sensor.SensorListenerService` | false | Monitors hardware step and motion sensors. |
| `com.nianticlabs.background.breadcrumb.SessionManagerService` | false | Manages active breadcrumb recording sessions. |
| `androidx.work.impl.background.systemjob.SystemJobService` | true (perm=BIND_JOB_SERVICE) | Android WorkManager periodic background synchronization for fitness data. |
| `com.nianticproject.holoholo.libholoholo.pushnotifications.NotificationListenerService` | false | FCM Push notification handler for raid invites, gifts, and event reminders. |

## 5. Broadcast Receivers
| Receiver Class | Exported | Triggers / Actions |
|----------------|----------|--------------------|
| `com.nianticproject.holoholo.libholoholo.appwidget.EggsWidget` | **true** | `android.appwidget.action.APPWIDGET_UPDATE` |
| `com.nianticproject.holoholo.libholoholo.appwidget.BuddyWidget` | **true** | `android.appwidget.action.APPWIDGET_UPDATE` |
| `com.nianticproject.holoholo.libholoholo.appwidget.DailyStreaksWidget` | **true** | `android.appwidget.action.APPWIDGET_UPDATE` |
| `com.nianticproject.holoholo.libholoholo.appwidget.ActivitySummaryWidget` | **true** | `android.appwidget.action.APPWIDGET_UPDATE` |
| `com.nianticlabs.platform.niaadvsynclocation.awareness.geofence.broadcastreceiver.GeofenceTransitionReceiver` | true | `com.google.android.gms.location.GEOFENCE_TRANSITION` |
| `com.nianticlabs.platform.niaadvsynclocation.awareness.activitymonitoring.broadcastreceiver.ActivityTransitionReceiver` | true | `com.google.android.gms.location.ACTIVITY_TRANSITION` |
| `com.nianticlabs.background.awareness.BootReceiver` | true | `android.intent.action.BOOT_COMPLETED` |
| `com.nianticlabs.background.sensor.SensorBootReceiver` | true | `android.intent.action.BOOT_COMPLETED` |

## 6. Content Providers
| Provider Class | Authority | Purpose |
|----------------|-----------|---------|
| `com.nianticlabs.platform.sharedlogin.LoginContentProvider` | `${applicationId}.sharedlogin` | Inter-process login credential sharing across Niantic games (Ingress, Pikmin Bloom, Monster Hunter Now). |
| `com.nianticlabs.bgcore.util.KeyValueContentProvider` | `${applicationId}.bgcore.util.contentProviderPermission` | Key-value data persistence accessed across main process and background services. |
| `androidx.core.content.FileProvider` | `${applicationId}.fileprovider` | Safe sharing of photo snapshots and buddy images with system apps. |

## 7. Native Libraries Inventory (arm64-v8a)
- `libil2cpp.so` (252.4 MB): The entire compiled C# codebase (game engine scripts, models, state machine, gameplay logic).
- `libholoholo.so` (13.4 MB): Niantic proprietary networking layer, RPC encryption, and anti-tamper client verification.
- `libNianticLabsPlugin.so` (13.0 MB): JNI bridge connecting Java Android APIs to C++/Unity.
- `libunity.so` (24.7 MB): Unity engine rendering, scene graph, physics, and asset management.
- `libadventuresync.so` (4.3 MB): Native step counter and trajectory validation.
- `libfmod.so` (1.5 MB) & `libfmodstudio.so` (1.3 MB): FMOD audio processing.
- `libarcore_sdk_c.so` (68 KB) & `libarcore_sdk_jni.so` (100 KB): Google ARCore native integration.
