package org.cleanroom.pokemongo.data.automation

import org.cleanroom.pokemongo.data.models.ItemType

val DEFAULT_ITEM_KEEP_LIMITS: Map<ItemType, Int> = mapOf(
    ItemType.POKE_BALL to 50,
    ItemType.GREAT_BALL to 100,
    ItemType.ULTRA_BALL to 150,
    ItemType.POTION to 20,
    ItemType.SUPER_POTION to 20,
    ItemType.HYPER_POTION to 50,
    ItemType.MAX_POTION to 50,
    ItemType.REVIVE to 20,
    ItemType.MAX_REVIVE to 50,
    ItemType.RAZZ_BERRY to 30,
    ItemType.NANAB_BERRY to 10,
    ItemType.PINAP_BERRY to 50
)

data class AutomationConfig(
    val autoCatchEnabled: Boolean = false,
    val autoSpinEnabled: Boolean = false,
    val autoFeedEnabled: Boolean = false,
    val preferredBall: ItemType = ItemType.POKE_BALL,
    val enhancedThrowEnabled: Boolean = false,
    val enhancedThrowQuality: org.cleanroom.pokemongo.data.repository.ThrowQuality = org.cleanroom.pokemongo.data.repository.ThrowQuality.EXCELLENT,
    val guaranteedCurveball: Boolean = true,
    val arPlusBonusEnabled: Boolean = false,
    val encounterIvEnabled: Boolean = true,
    val caughtPreviewEnabled: Boolean = true,
    val inventoryIvBadgesEnabled: Boolean = true,
    val joystickEnabled: Boolean = true,
    val speedControlsEnabled: Boolean = true,
    val catchRangeMeters: Double = 80.0,
    val spinRangeMeters: Double = 40.0,
    val feedThresholdPercent: Int = 30,
    // Extended Mod Features
    val blockNonShinyEnabled: Boolean = false,
    val blockNonHundoEnabled: Boolean = false,
    val quickCatchEnabled: Boolean = false,
    val autoDiscardEnabled: Boolean = false,
    val itemKeepLimits: Map<ItemType, Int> = DEFAULT_ITEM_KEEP_LIMITS,
    val transferOnCatchEnabled: Boolean = false,
    val transferIvThreshold: Float = 90.0f,
    val keepShinyOnCatch: Boolean = true,
    val keepHundoOnCatch: Boolean = true,
    val keepLegendaryOnCatch: Boolean = true,
    val keepPvPRankOnCatch: Boolean = true,
    val autoIncubatorEnabled: Boolean = false,
    val eggIncubatorPriority: String = "SHORTEST_FIRST",
    val giftAssistantEnabled: Boolean = false,
    val questAssistantEnabled: Boolean = false,
    val buddyAssistantExtended: Boolean = false,
    val spawnBoosterMultiplier: Int = 1,
    val hideTrainerName: Boolean = false,
    val hideAppWithGestureEnabled: Boolean = true,
    val customShortcuts: List<String> = listOf("SNIPER", "ROUTES", "TOOLS", "HEAL", "SPAWNS"),
    val appLanguage: String = "English",
    val pokemonFeedLanguage: String = "English",
    val gymBattleAssistEnabled: Boolean = false,
    val dynamaxAssistEnabled: Boolean = false
)

sealed class AutoCatchEvent {
    data class Captured(val speciesName: String, val cp: Int, val ballUsed: ItemType) : AutoCatchEvent()
    data class Escaped(val speciesName: String) : AutoCatchEvent()
    object OutOfBalls : AutoCatchEvent()
    object BagFull : AutoCatchEvent()
}

sealed class AutoSpinEvent {
    data class Spun(val pokestopName: String, val itemsCount: Int, val xp: Int) : AutoSpinEvent()
}

sealed class AutoFeedEvent {
    data class Fed(val berryUsed: ItemType, val newHunger: Int) : AutoFeedEvent()
}

data class CaughtPreviewPrediction(
    val isLikelyCaught: Boolean,
    val probability: Float,
    val message: String
)

data class EncounterIvDetails(
    val ivAttack: Int,
    val ivDefense: Int,
    val ivStamina: Int,
    val ivPercent: Float,
    val stars: Int,
    val exactCp: Int
)

sealed class EncounterFilterResult {
    object Allowed : EncounterFilterResult()
    data class Blocked(val reason: String) : EncounterFilterResult()
}

data class DiscardReport(
    val totalDiscarded: Int,
    val discardedDetails: Map<ItemType, Int>
)

data class TransferOnCatchEvent(
    val speciesName: String,
    val cp: Int,
    val ivPercent: Float,
    val candiesAwarded: Int
)

data class GiftAssistantResult(
    val giftsOpened: Int,
    val giftsSent: Int,
    val stardustAwarded: Int,
    val xpAwarded: Int
)

data class AutoIncubateResult(
    val assignedCount: Int,
    val details: List<String>
)
