package org.cleanroom.pokemongo.data.models

enum class BuddyEmotion(val title: String, val drawableResName: String) {
    DULL("Tired", "buddy_emotion_dull"),
    NORMAL("Normal", "buddy_emotion_normal"),
    SMILE("Happy", "buddy_emotion_smile"),
    HAPPY("Joyful", "buddy_emotion_happy"),
    EXCITED("Excited", "buddy_emotion_yatta")
}

data class Buddy(
    var pokemonInstanceId: String? = null,
    var affectionKmProgress: Float = 0.0f,
    var totalKmWalked: Float = 0.0f,
    var totalCandiesFound: Int = 0,
    var hungerPercent: Int = 100, // 0 = hungry, 100 = full
    var emotion: BuddyEmotion = BuddyEmotion.NORMAL,
    var heartsWalk: Int = 0,     // max 3
    var heartsFeed: Int = 0,     // max 3
    var heartsPlay: Int = 0,     // max 1
    var heartsBattle: Int = 0,   // max 1
    var hasGift: Boolean = false
) {
    fun getTotalHeartsToday(): Int = heartsWalk + heartsFeed + heartsPlay + heartsBattle

    fun getAffectionLevelName(): String = when {
        getTotalHeartsToday() >= 20 -> "Best Buddy"
        getTotalHeartsToday() >= 12 -> "Ultra Buddy"
        getTotalHeartsToday() >= 6 -> "Great Buddy"
        getTotalHeartsToday() >= 1 -> "Good Buddy"
        else -> "Buddy"
    }
}
