package org.cleanroom.pokemongo.data.activation

enum class ActivationTier(val displayName: String) {
    FREE("Standard Edition"),
    PRO("Pro Edition (Activated)")
}

enum class ProFeature(val displayName: String, val description: String) {
    JOYSTICK("Virtual Joystick", "On-screen floating thumbstick for fine movement"),
    SPEED_CONTROLS("Speed-Up Controls", "Switch between Walk (9.3km/h), Run (18.5km/h), Cycle (30km/h), and Drive (60km/h)"),
    DOUBLE_TAP_TELEPORT("Double Tap to Teleport", "Instant map teleportation with distance cooldown indicator"),
    ENHANCED_THROW("Enhanced Throw", "Guarantees 100% Excellent and Curveball throw scoring"),
    ENCOUNTER_IV("Encounter IV Display", "Real-time wild Pokémon IV% and exact stat HUD during encounters"),
    CAUGHT_PREVIEW("Caught Preview", "Instant catch success or flee notification without waiting through animations"),
    AUTO_CATCH("Auto-Catch Engine", "Automated capture of wild Pokémon in range"),
    AUTO_SPIN("Auto-Spin PokéStops", "Automated spinning of in-range PokéStops with loot aggregation"),
    AUTO_FEED("Auto-Feed Buddy", "Automatically feeds berries when buddy hunger drops below 30%"),
    INVENTORY_IV("Inventory IV Badges", "Displays IV percentage badge on all Pokémon cards in Box")
}

sealed class ActivationState {
    data class Inactive(val reason: String = "No active license key") : ActivationState()
    data class Active(
        val licenseKey: String,
        val expiryTimestamp: Long,
        val tier: ActivationTier = ActivationTier.PRO
    ) : ActivationState() {
        fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean {
            return expiryTimestamp > 0 && currentTime > expiryTimestamp
        }

        fun remainingDays(currentTime: Long = System.currentTimeMillis()): Long {
            if (expiryTimestamp <= 0) return 9999L // Lifetime
            val diff = expiryTimestamp - currentTime
            return (diff / (1000 * 60 * 60 * 24L)).coerceAtLeast(0L)
        }
    }
}
