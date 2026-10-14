package org.cleanroom.pokemongo.data.models

enum class ItemCategory {
    BALL, POTION, REVIVE, BERRY, SPECIAL
}

enum class ItemType(
    val id: String,
    val displayName: String,
    val category: ItemCategory,
    val description: String,
    val catchMultiplier: Float = 1.0f,
    val healAmount: Int = 0
) {
    POKE_BALL("poke_ball", "Poké Ball", ItemCategory.BALL, "A device for catching wild Pokémon.", catchMultiplier = 1.0f),
    GREAT_BALL("great_ball", "Great Ball", ItemCategory.BALL, "A good, high-performance Poké Ball that provides a higher success rate.", catchMultiplier = 1.5f),
    ULTRA_BALL("ultra_ball", "Ultra Ball", ItemCategory.BALL, "An ultra-high-performance Poké Ball with superior catch rate.", catchMultiplier = 2.0f),

    POTION("potion", "Potion", ItemCategory.POTION, "Restores the HP of one Pokémon by 20 points.", healAmount = 20),
    SUPER_POTION("super_potion", "Super Potion", ItemCategory.POTION, "Restores the HP of one Pokémon by 50 points.", healAmount = 50),
    HYPER_POTION("hyper_potion", "Hyper Potion", ItemCategory.POTION, "Restores the HP of one Pokémon by 200 points.", healAmount = 200),
    MAX_POTION("max_potion", "Max Potion", ItemCategory.POTION, "Completely restores the HP of one Pokémon.", healAmount = 9999),

    REVIVE("revive", "Revive", ItemCategory.REVIVE, "Revives a fainted Pokémon and restores half of its max HP."),
    MAX_REVIVE("max_revive", "Max Revive", ItemCategory.REVIVE, "Revives a fainted Pokémon and completely restores its max HP."),

    RAZZ_BERRY("razz_berry", "Razz Berry", ItemCategory.BERRY, "Feed this to a wild Pokémon to make it easier to catch.", catchMultiplier = 1.5f),
    NANAB_BERRY("nanab_berry", "Nanab Berry", ItemCategory.BERRY, "Feed this to a wild Pokémon to calm it down and slow its movements."),
    PINAP_BERRY("pinap_berry", "Pinap Berry", ItemCategory.BERRY, "Feed this to a wild Pokémon to earn double Candy if caught!"),

    INCENSE("incense", "Incense", ItemCategory.SPECIAL, "An alluring fragrance that attracts wild Pokémon to your location for 30 minutes."),
    LUCKY_EGG("lucky_egg", "Lucky Egg", ItemCategory.SPECIAL, "Earns double XP for 30 minutes.")
}

data class ItemInventoryEntry(
    val itemType: ItemType,
    var count: Int
)
