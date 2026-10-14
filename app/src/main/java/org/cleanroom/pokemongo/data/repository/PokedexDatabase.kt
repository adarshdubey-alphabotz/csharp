package org.cleanroom.pokemongo.data.repository

import org.cleanroom.pokemongo.data.models.PokemonSpecies
import org.cleanroom.pokemongo.data.models.PokemonType

object PokedexDatabase {

    val speciesList: List<PokemonSpecies> = listOf(
        PokemonSpecies(
            id = 1,
            name = "Bulbasaur",
            primaryType = PokemonType.GRASS,
            secondaryType = PokemonType.POISON,
            baseAttack = 118,
            baseDefense = 111,
            baseStamina = 128,
            baseCaptureRate = 0.20f,
            candyToEvolve = 25,
            evolvesToSpeciesId = 2,
            heightM = 0.7f,
            weightKg = 6.9f,
            flavorText = "A strange seed was planted on its back at birth. The plant sprouts and grows with this Pokémon.",
            buddyKmPerCandy = 3.0f
        ),
        PokemonSpecies(
            id = 2,
            name = "Ivysaur",
            primaryType = PokemonType.GRASS,
            secondaryType = PokemonType.POISON,
            baseAttack = 151,
            baseDefense = 143,
            baseStamina = 155,
            baseCaptureRate = 0.10f,
            candyToEvolve = 100,
            evolvesToSpeciesId = 3,
            heightM = 1.0f,
            weightKg = 13.0f,
            flavorText = "When the bulb on its back grows large, it appears to lose the ability to stand on its hind legs.",
            buddyKmPerCandy = 3.0f
        ),
        PokemonSpecies(
            id = 3,
            name = "Venusaur",
            primaryType = PokemonType.GRASS,
            secondaryType = PokemonType.POISON,
            baseAttack = 198,
            baseDefense = 189,
            baseStamina = 190,
            baseCaptureRate = 0.05f,
            candyToEvolve = 0,
            evolvesToSpeciesId = null,
            heightM = 2.0f,
            weightKg = 100.0f,
            flavorText = "The flower on its back catches the sun's rays and turns them into energy to power its attacks.",
            buddyKmPerCandy = 3.0f
        ),
        PokemonSpecies(
            id = 4,
            name = "Charmander",
            primaryType = PokemonType.FIRE,
            baseAttack = 116,
            baseDefense = 93,
            baseStamina = 118,
            baseCaptureRate = 0.20f,
            candyToEvolve = 25,
            evolvesToSpeciesId = 5,
            heightM = 0.6f,
            weightKg = 8.5f,
            flavorText = "The flame on its tail indicates Charmander's life force. If it is healthy, the flame burns brightly.",
            buddyKmPerCandy = 3.0f
        ),
        PokemonSpecies(
            id = 5,
            name = "Charmeleon",
            primaryType = PokemonType.FIRE,
            baseAttack = 158,
            baseDefense = 126,
            baseStamina = 151,
            baseCaptureRate = 0.10f,
            candyToEvolve = 100,
            evolvesToSpeciesId = 6,
            heightM = 1.1f,
            weightKg = 19.0f,
            flavorText = "It lashes about with its tail to knock down foes. It then tears up the fallen opponent with sharp claws.",
            buddyKmPerCandy = 3.0f
        ),
        PokemonSpecies(
            id = 6,
            name = "Charizard",
            primaryType = PokemonType.FIRE,
            secondaryType = PokemonType.FLYING,
            baseAttack = 223,
            baseDefense = 173,
            baseStamina = 186,
            baseCaptureRate = 0.05f,
            candyToEvolve = 0,
            evolvesToSpeciesId = null,
            heightM = 1.7f,
            weightKg = 90.5f,
            flavorText = "It spits fire that is hot enough to melt boulders. It may cause forest fires by blowing flames.",
            buddyKmPerCandy = 3.0f
        ),
        PokemonSpecies(
            id = 7,
            name = "Squirtle",
            primaryType = PokemonType.WATER,
            baseAttack = 94,
            baseDefense = 121,
            baseStamina = 127,
            baseCaptureRate = 0.20f,
            candyToEvolve = 25,
            evolvesToSpeciesId = 8,
            heightM = 0.5f,
            weightKg = 9.0f,
            flavorText = "Shoots water at prey while in the water. Withdraws into its shell when in danger.",
            buddyKmPerCandy = 3.0f
        ),
        PokemonSpecies(
            id = 8,
            name = "Wartortle",
            primaryType = PokemonType.WATER,
            baseAttack = 126,
            baseDefense = 155,
            baseStamina = 153,
            baseCaptureRate = 0.10f,
            candyToEvolve = 100,
            evolvesToSpeciesId = 9,
            heightM = 1.0f,
            weightKg = 22.5f,
            flavorText = "When tapped, this Pokémon will pull in its head, but its tail will still stick out a little bit.",
            buddyKmPerCandy = 3.0f
        ),
        PokemonSpecies(
            id = 9,
            name = "Blastoise",
            primaryType = PokemonType.WATER,
            baseAttack = 171,
            baseDefense = 207,
            baseStamina = 188,
            baseCaptureRate = 0.05f,
            candyToEvolve = 0,
            evolvesToSpeciesId = null,
            heightM = 1.6f,
            weightKg = 85.5f,
            flavorText = "A brutal Pokémon with pressurized water jets on its shell. They are used for high-speed tackles.",
            buddyKmPerCandy = 3.0f
        ),
        PokemonSpecies(
            id = 16,
            name = "Pidgey",
            primaryType = PokemonType.NORMAL,
            secondaryType = PokemonType.FLYING,
            baseAttack = 85,
            baseDefense = 73,
            baseStamina = 120,
            baseCaptureRate = 0.50f,
            candyToEvolve = 12,
            evolvesToSpeciesId = 17,
            heightM = 0.3f,
            weightKg = 1.8f,
            flavorText = "A common sight in forests and woods. It flaps its wings at ground level to kick up blinding sand.",
            buddyKmPerCandy = 1.0f
        ),
        PokemonSpecies(
            id = 17,
            name = "Pidgeotto",
            primaryType = PokemonType.NORMAL,
            secondaryType = PokemonType.FLYING,
            baseAttack = 117,
            baseDefense = 105,
            baseStamina = 160,
            baseCaptureRate = 0.25f,
            candyToEvolve = 50,
            evolvesToSpeciesId = 18,
            heightM = 1.1f,
            weightKg = 30.0f,
            flavorText = "Very protective of its sprawling territorial area, this Pokémon will fiercely peck at any intruder.",
            buddyKmPerCandy = 1.0f
        ),
        PokemonSpecies(
            id = 18,
            name = "Pidgeot",
            primaryType = PokemonType.NORMAL,
            secondaryType = PokemonType.FLYING,
            baseAttack = 166,
            baseDefense = 154,
            baseStamina = 195,
            baseCaptureRate = 0.12f,
            candyToEvolve = 0,
            evolvesToSpeciesId = null,
            heightM = 1.5f,
            weightKg = 39.5f,
            flavorText = "When hunting, it skims the surface of water at high speed to pick off unwary prey such as Magikarp.",
            buddyKmPerCandy = 1.0f
        ),
        PokemonSpecies(
            id = 25,
            name = "Pikachu",
            primaryType = PokemonType.ELECTRIC,
            baseAttack = 112,
            baseDefense = 96,
            baseStamina = 111,
            baseCaptureRate = 0.20f,
            candyToEvolve = 50,
            evolvesToSpeciesId = 26,
            heightM = 0.4f,
            weightKg = 6.0f,
            flavorText = "When several of these Pokémon gather, their electricity could build and cause lightning storms.",
            buddyKmPerCandy = 1.0f
        ),
        PokemonSpecies(
            id = 26,
            name = "Raichu",
            primaryType = PokemonType.ELECTRIC,
            baseAttack = 193,
            baseDefense = 151,
            baseStamina = 155,
            baseCaptureRate = 0.10f,
            candyToEvolve = 0,
            evolvesToSpeciesId = null,
            heightM = 0.8f,
            weightKg = 30.0f,
            flavorText = "Its long tail serves as a ground to protect itself from its own high-voltage electrical power.",
            buddyKmPerCandy = 1.0f
        ),
        PokemonSpecies(
            id = 92,
            name = "Gastly",
            primaryType = PokemonType.GHOST,
            secondaryType = PokemonType.POISON,
            baseAttack = 186,
            baseDefense = 67,
            baseStamina = 102,
            baseCaptureRate = 0.40f,
            candyToEvolve = 25,
            evolvesToSpeciesId = 93,
            heightM = 1.3f,
            weightKg = 0.1f,
            flavorText = "Almost invisible, this gaseous Pokémon cloaks the target and puts it to sleep without notice.",
            buddyKmPerCandy = 3.0f
        ),
        PokemonSpecies(
            id = 93,
            name = "Haunter",
            primaryType = PokemonType.GHOST,
            secondaryType = PokemonType.POISON,
            baseAttack = 223,
            baseDefense = 107,
            baseStamina = 128,
            baseCaptureRate = 0.20f,
            candyToEvolve = 100,
            evolvesToSpeciesId = 94,
            heightM = 1.6f,
            weightKg = 0.1f,
            flavorText = "Because of its ability to slip through block walls, it is said to be from another dimension.",
            buddyKmPerCandy = 3.0f
        ),
        PokemonSpecies(
            id = 94,
            name = "Gengar",
            primaryType = PokemonType.GHOST,
            secondaryType = PokemonType.POISON,
            baseAttack = 261,
            baseDefense = 149,
            baseStamina = 155,
            baseCaptureRate = 0.10f,
            candyToEvolve = 0,
            evolvesToSpeciesId = null,
            heightM = 1.5f,
            weightKg = 40.5f,
            flavorText = "Under a full moon, this Pokémon likes to mimic the shadows of people and laugh at their fright.",
            buddyKmPerCandy = 3.0f
        ),
        PokemonSpecies(
            id = 129,
            name = "Magikarp",
            primaryType = PokemonType.WATER,
            baseAttack = 29,
            baseDefense = 85,
            baseStamina = 85,
            baseCaptureRate = 0.70f,
            candyToEvolve = 400,
            evolvesToSpeciesId = 130,
            heightM = 0.9f,
            weightKg = 10.0f,
            flavorText = "In the distant past, it was somewhat stronger than the horribly weak descendants that exist today.",
            buddyKmPerCandy = 1.0f
        ),
        PokemonSpecies(
            id = 130,
            name = "Gyarados",
            primaryType = PokemonType.WATER,
            secondaryType = PokemonType.FLYING,
            baseAttack = 237,
            baseDefense = 186,
            baseStamina = 216,
            baseCaptureRate = 0.10f,
            candyToEvolve = 0,
            evolvesToSpeciesId = null,
            heightM = 6.5f,
            weightKg = 235.0f,
            flavorText = "Rarely seen in the wild. Huge and vicious, it is capable of destroying entire cities in a rage.",
            buddyKmPerCandy = 1.0f
        ),
        PokemonSpecies(
            id = 133,
            name = "Eevee",
            primaryType = PokemonType.NORMAL,
            baseAttack = 104,
            baseDefense = 114,
            baseStamina = 146,
            baseCaptureRate = 0.40f,
            candyToEvolve = 25,
            evolvesToSpeciesId = 134,
            heightM = 0.3f,
            weightKg = 6.5f,
            flavorText = "Its genetic code is irregular. It may mutate if it is exposed to radiation from element stones.",
            buddyKmPerCandy = 5.0f
        ),
        PokemonSpecies(
            id = 134,
            name = "Vaporeon",
            primaryType = PokemonType.WATER,
            baseAttack = 205,
            baseDefense = 161,
            baseStamina = 277,
            baseCaptureRate = 0.12f,
            candyToEvolve = 0,
            evolvesToSpeciesId = null,
            heightM = 1.0f,
            weightKg = 29.0f,
            flavorText = "Lives close to water. Its long tail is ridged with a fin which is often mistaken for a mermaid's.",
            buddyKmPerCandy = 5.0f
        ),
        PokemonSpecies(
            id = 143,
            name = "Snorlax",
            primaryType = PokemonType.NORMAL,
            baseAttack = 190,
            baseDefense = 169,
            baseStamina = 330,
            baseCaptureRate = 0.05f,
            candyToEvolve = 0,
            evolvesToSpeciesId = null,
            heightM = 2.1f,
            weightKg = 460.0f,
            flavorText = "Very lazy. Just eats and sleeps. As its rotund bulk builds, it becomes steadily more slothful.",
            buddyKmPerCandy = 5.0f
        ),
        PokemonSpecies(
            id = 147,
            name = "Dratini",
            primaryType = PokemonType.DRAGON,
            baseAttack = 115,
            baseDefense = 91,
            baseStamina = 121,
            baseCaptureRate = 0.40f,
            candyToEvolve = 25,
            evolvesToSpeciesId = 148,
            heightM = 1.8f,
            weightKg = 3.3f,
            flavorText = "Long considered a mythical Pokémon until recently, when a small colony was found living underwater.",
            buddyKmPerCandy = 5.0f
        ),
        PokemonSpecies(
            id = 148,
            name = "Dragonair",
            primaryType = PokemonType.DRAGON,
            baseAttack = 163,
            baseDefense = 135,
            baseStamina = 156,
            baseCaptureRate = 0.20f,
            candyToEvolve = 100,
            evolvesToSpeciesId = 149,
            heightM = 4.0f,
            weightKg = 16.5f,
            flavorText = "A mystical Pokémon that exudes a gentle aura. It has the ability to change the weather on a whim.",
            buddyKmPerCandy = 5.0f
        ),
        PokemonSpecies(
            id = 149,
            name = "Dragonite",
            primaryType = PokemonType.DRAGON,
            secondaryType = PokemonType.FLYING,
            baseAttack = 263,
            baseDefense = 198,
            baseStamina = 209,
            baseCaptureRate = 0.05f,
            candyToEvolve = 0,
            evolvesToSpeciesId = null,
            heightM = 2.2f,
            weightKg = 210.0f,
            flavorText = "An extremely rarely seen marine Pokémon. Its intelligence is said to match that of humans.",
            buddyKmPerCandy = 5.0f
        ),
        PokemonSpecies(
            id = 150,
            name = "Mewtwo",
            primaryType = PokemonType.PSYCHIC,
            baseAttack = 300,
            baseDefense = 182,
            baseStamina = 214,
            baseCaptureRate = 0.02f,
            candyToEvolve = 0,
            evolvesToSpeciesId = null,
            heightM = 2.0f,
            weightKg = 122.0f,
            flavorText = "It was created by a scientist after years of horrific gene splicing and DNA engineering experiments.",
            buddyKmPerCandy = 20.0f
        )
    )

    private val speciesMap = speciesList.associateBy { it.id }

    fun getById(id: Int): PokemonSpecies? = speciesMap[id]

    private val japaneseNames = mapOf(
        1 to "フシギダネ (Fushigidane)", 2 to "フシギソウ (Fushigisou)", 3 to "フシギバナ (Fushigibana)",
        4 to "ヒトカゲ (Hitokage)", 5 to "リザード (Lizardo)", 6 to "リザードン (Lizardon)",
        7 to "ゼニガメ (Zenigame)", 8 to "カメール (Kameil)", 9 to "カメックス (Kamex)",
        25 to "ピカチュウ (Pikachu)", 26 to "ライチュウ (Raichu)", 68 to "カイリキー (Kairiky)",
        94 to "ゲンガー (Gangar)", 130 to "ギャラドス (Gyarados)", 131 to "ラプラス (Laplace)",
        143 to "カビゴン (Kabigon)", 149 to "カイリュー (Kairyu)", 150 to "ミュウツー (Mewtwo)"
    )

    private val germanNames = mapOf(
        1 to "Bisasam", 2 to "Bisaknosp", 3 to "Bisaflor",
        4 to "Glumanda", 5 to "Glutexo", 6 to "Glurak",
        7 to "Schiggy", 8 to "Schillok", 9 to "Turtok",
        25 to "Pikachu", 68 to "Machomei", 94 to "Gengar",
        130 to "Garados", 131 to "Lapras", 143 to "Relaxo",
        149 to "Dragoran", 150 to "Mewtu"
    )

    private val frenchNames = mapOf(
        1 to "Bulbizarre", 2 to "Herbizarre", 3 to "Florizarre",
        4 to "Salamèche", 5 to "Reptincel", 6 to "Dracaufeu",
        7 to "Carapuce", 8 to "Carabaffe", 9 to "Tortank",
        25 to "Pikachu", 68 to "Mackogneur", 94 to "Ectoplasma",
        130 to "Léviator", 131 to "Lokhlass", 143 to "Ronflex",
        149 to "Dracolosse", 150 to "Mewtwo"
    )

    fun getLocalizedName(speciesId: Int, language: String): String {
        val baseName = getById(speciesId)?.name ?: "Pokémon"
        return when (language.lowercase()) {
            "japanese", "日本語", "original (japanese)" -> japaneseNames[speciesId] ?: baseName
            "german", "deutsch", "german names" -> germanNames[speciesId] ?: baseName
            "french", "français", "french names" -> frenchNames[speciesId] ?: baseName
            else -> baseName
        }
    }

    fun getRandomSpawnSpecies(): PokemonSpecies {
        // Weighted selection favoring common species (Pidgey, Magikarp, Rattata, Bulbasaur, etc.)
        val pool = listOf(
            1, 1, 4, 4, 7, 7, 16, 16, 16, 16, 25, 25, 92, 92, 129, 129, 129, 133, 133, 143, 147
        )
        val selectedId = pool.random()
        return getById(selectedId) ?: speciesList.first()
    }
}
