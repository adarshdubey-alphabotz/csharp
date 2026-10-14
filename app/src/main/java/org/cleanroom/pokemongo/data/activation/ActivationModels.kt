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
    INVENTORY_IV("Inventory IV Badges", "Displays IV percentage badge on all Pokémon cards in Box"),
    BLOCK_NON_SHINY("Block Non-Shiny", "Automatically blocks or flees encounters that are not shiny"),
    BLOCK_NON_HUNDO("Block Non-Hundo", "Automatically blocks encounters with IV less than 100%"),
    QUICK_CATCH("Quick Catch", "Immediately skips the 3-shake ball animation and adds caught Pokémon"),
    QUICK_SNIPER("Quick Sniper I & II", "Live global coordinate feeds for 100% IV and top PvP rank Pokémon"),
    AUTO_DISCARD("Auto-Discard Items", "Automatically cleans inventory exceeding configured item limits"),
    TRANSFER_ON_CATCH("Transfer on Catch", "Automatically transfers caught Pokémon below IV threshold while keeping shinies and favorites"),
    AUTO_INCUBATOR("Auto-Incubator", "Automatically assigns eggs to infinite and super incubators by priority"),
    GIFT_ASSISTANT("Gift Assistant", "Batch opens and sends daily friend gifts in one click"),
    QUEST_ASSISTANT("Quest Assistant", "Automatically triggers snapshot and interaction tasks for field research"),
    ROUTE_WALK("Route Walk & GPX", "Multi-point waypoint patrol and GPX route walking engine"),
    PVP_RANK_CALCULATOR("PvP Rank Calculator", "Calculates Great League and Ultra League stat product ranks"),
    NAME_GENERATOR("Pokémon Name Generator", "Generates custom renaming templates with IV, stats, and moves"),
    SPAWN_BOOSTER("Spawn Booster", "Increases local wild Pokémon spawn cluster density"),
    BATTLE_ASSISTANT("Raid & Gym Battle Assistant", "Automates Fast Attacks, Charged Moves, and dodge timing")
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
