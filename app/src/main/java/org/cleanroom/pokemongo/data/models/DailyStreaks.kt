package org.cleanroom.pokemongo.data.models

data class DailyStreaks(
    var catchStreak: Int = 1,
    var spinStreak: Int = 1,
    var lastCatchDate: String = "",
    var lastSpinDate: String = ""
) {
    fun isCatchCompletedToday(today: String): Boolean = lastCatchDate == today
    fun isSpinCompletedToday(today: String): Boolean = lastSpinDate == today
}
