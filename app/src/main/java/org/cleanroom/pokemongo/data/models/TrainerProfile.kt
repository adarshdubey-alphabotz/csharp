package org.cleanroom.pokemongo.data.models

data class TrainerProfile(
    var name: String = "Red",
    var level: Int = 28,
    var currentXp: Long = 185000L,
    var requiredXpForNextLevel: Long = 300000L,
    var stardust: Int = 145000,
    var pokeCoins: Int = 420,
    var team: Team = Team.MYSTIC,
    var totalCatches: Int = 1240,
    var totalPokestopsSpun: Int = 890,
    var totalKmWalked: Float = 248.5f,
    var totalEggsHatched: Int = 64,
    var totalEvolutions: Int = 112
) {
    fun addXp(amount: Long) {
        currentXp += amount
        if (currentXp >= requiredXpForNextLevel && level < 50) {
            level++
            requiredXpForNextLevel = (requiredXpForNextLevel * 1.35).toLong()
        }
    }
}
