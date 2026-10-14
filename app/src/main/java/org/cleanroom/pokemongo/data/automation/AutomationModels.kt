package org.cleanroom.pokemongo.data.automation

import org.cleanroom.pokemongo.data.models.ItemType

data class AutomationConfig(
    val autoCatchEnabled: Boolean = false,
    val autoSpinEnabled: Boolean = false,
    val autoFeedEnabled: Boolean = false,
    val preferredBall: ItemType = ItemType.POKE_BALL,
    val enhancedThrowEnabled: Boolean = false,
    val guaranteedCurveball: Boolean = true,
    val encounterIvEnabled: Boolean = true,
    val caughtPreviewEnabled: Boolean = true,
    val inventoryIvBadgesEnabled: Boolean = true,
    val joystickEnabled: Boolean = true,
    val speedControlsEnabled: Boolean = true,
    val catchRangeMeters: Double = 80.0,
    val spinRangeMeters: Double = 40.0,
    val feedThresholdPercent: Int = 30
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
