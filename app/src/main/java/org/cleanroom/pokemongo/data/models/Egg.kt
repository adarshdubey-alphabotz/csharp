package org.cleanroom.pokemongo.data.models

import java.util.UUID

enum class EggTier(val distanceKm: Float, val drawableResName: String) {
    TWO_KM(2.0f, "egg_2km"),
    FIVE_KM(5.0f, "egg_5km"),
    SEVEN_KM(7.0f, "egg_7km"),
    TEN_KM(10.0f, "egg_10km"),
    TWELVE_KM(12.0f, "egg_12km")
}

data class Egg(
    val id: String = UUID.randomUUID().toString(),
    val tier: EggTier,
    val acquiredTimestamp: Long = System.currentTimeMillis(),
    var walkedKm: Float = 0.0f,
    var incubatorId: String? = null,
    var isHatched: Boolean = false
) {
    fun isReadyToHatch(): Boolean = walkedKm >= tier.distanceKm
}

data class Incubator(
    val id: String = UUID.randomUUID().toString(),
    val isUnlimited: Boolean = false,
    val distanceMultiplier: Float = 1.0f, // 1.0 for Basic, 0.67 for Super
    var currentEggId: String? = null,
    var usesRemaining: Int = 3 // Unlimited has infinite uses
) {
    fun isAvailable(): Boolean = currentEggId == null && (isUnlimited || usesRemaining > 0)
}
