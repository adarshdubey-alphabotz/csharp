package org.cleanroom.pokemongo.data.naming

import org.cleanroom.pokemongo.data.models.PokemonInstance
import org.cleanroom.pokemongo.data.pvp.PvpLeague
import org.cleanroom.pokemongo.data.pvp.PvpRankCalculator
import org.cleanroom.pokemongo.data.repository.PokedexDatabase

object PokemonNameGenerator {

    val TEMPLATE_IV_STATS = "{iv}_{atk}/{def}/{sta}"
    val TEMPLATE_NAME_IV = "{name}_{iv}"
    val TEMPLATE_PVP_GL = "{glRank}_{name}_{iv}"
    val TEMPLATE_FULL = "{name}_{iv}_{fastMove:3}/{chargedMove:3}"

    fun generateName(
        pokemon: PokemonInstance,
        template: String = TEMPLATE_IV_STATS
    ): String {
        val species = PokedexDatabase.getById(pokemon.speciesId)
        val name = species?.name ?: "Pokémon"
        val ivInt = pokemon.ivPercent.toInt()
        val atk = pokemon.ivAttack
        val def = pokemon.ivDefense
        val sta = pokemon.ivStamina

        var result = template
            .replace("{name}", name)
            .replace("{iv}", ivInt.toString())
            .replace("{atk}", atk.toString())
            .replace("{def}", def.toString())
            .replace("{sta}", sta.toString())
            .replace("{level}", pokemon.level.toInt().toString())
            .replace("{fastMove}", pokemon.fastMove)
            .replace("{chargedMove}", pokemon.chargedMove)
            .replace("{fastMove:3}", pokemon.fastMove.take(3))
            .replace("{chargedMove:3}", pokemon.chargedMove.take(3))

        if (result.contains("{glRank}")) {
            val pvp = PvpRankCalculator.calculatePvpRank(pokemon.speciesId, atk, def, sta, PvpLeague.GREAT_LEAGUE)
            result = result.replace("{glRank}", "#${pvp.rank}")
        }

        return result.take(20) // Pokémon nickname limit
    }
}
