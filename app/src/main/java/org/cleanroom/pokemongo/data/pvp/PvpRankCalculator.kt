package org.cleanroom.pokemongo.data.pvp

import org.cleanroom.pokemongo.data.repository.PokedexDatabase
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt

enum class PvpLeague(val displayName: String, val maxCp: Int) {
    GREAT_LEAGUE("Great League", 1500),
    ULTRA_LEAGUE("Ultra League", 2500),
    MASTER_LEAGUE("Master League", 10000)
}

data class PvpRankResult(
    val league: PvpLeague,
    val rank: Int,
    val optimalLevel: Float,
    val cpAtLevel: Int,
    val statProduct: Double,
    val statProductPercent: Float // e.g. 99.8% of Rank 1
)

object PvpRankCalculator {

    // Levels 1.0 to 40.0 with 0.5 step
    private fun getCpmForLevel(lvl: Float): Float {
        return when {
            lvl <= 1.0f -> 0.094f
            lvl <= 10.0f -> 0.094f + (lvl - 1.0f) * 0.0365f
            lvl <= 20.0f -> 0.4225f + (lvl - 10.0f) * 0.0175f
            lvl <= 30.0f -> 0.5974f + (lvl - 20.0f) * 0.0134f
            else -> (0.7317f + (lvl - 30.0f) * 0.0058f).coerceAtMost(0.7903f)
        }
    }

    private fun calculateCp(baseAtk: Int, ivAtk: Int, baseDef: Int, ivDef: Int, baseSta: Int, ivSta: Int, cpm: Float): Int {
        val attack = baseAtk + ivAtk
        val defense = baseDef + ivDef
        val stamina = baseSta + ivSta
        val rawCp = (attack * sqrt(defense.toDouble()) * sqrt(stamina.toDouble()) * (cpm * cpm)) / 10.0
        return max(10, floor(rawCp).toInt())
    }

    private fun calculateStatProduct(baseAtk: Int, ivAtk: Int, baseDef: Int, ivDef: Int, baseSta: Int, ivSta: Int, cpm: Float): Double {
        val attack = (baseAtk + ivAtk) * cpm.toDouble()
        val defense = (baseDef + ivDef) * cpm.toDouble()
        val hp = floor((baseSta + ivSta) * cpm.toDouble())
        return attack * defense * hp
    }


    /**
     * Finds highest level <= maxLevel where CP <= league.maxCp.
     */
    private fun findOptimalLevel(
        baseAtk: Int, ivAtk: Int,
        baseDef: Int, ivDef: Int,
        baseSta: Int, ivSta: Int,
        maxCp: Int
    ): Pair<Float, Float> {
        var bestLevel = 1.0f
        var bestCpm = getCpmForLevel(1.0f)

        var l = 1.0f
        while (l <= 40.0f) {
            val cpm = getCpmForLevel(l)
            val cp = calculateCp(baseAtk, ivAtk, baseDef, ivDef, baseSta, ivSta, cpm)
            if (cp <= maxCp) {
                bestLevel = l
                bestCpm = cpm
            } else {
                break
            }
            l += 0.5f
        }
        return Pair(bestLevel, bestCpm)
    }

    fun calculatePvpRank(speciesId: Int, ivAtk: Int, ivDef: Int, ivSta: Int, league: PvpLeague): PvpRankResult {
        val species = PokedexDatabase.getById(speciesId) ?: PokedexDatabase.speciesList.first()
        val baseAtk = species.baseAttack
        val baseDef = species.baseDefense
        val baseSta = species.baseStamina

        val (optLvl, optCpm) = findOptimalLevel(baseAtk, ivAtk, baseDef, ivDef, baseSta, ivSta, league.maxCp)
        val userStatProduct = calculateStatProduct(baseAtk, ivAtk, baseDef, ivDef, baseSta, ivSta, optCpm)
        val cp = calculateCp(baseAtk, ivAtk, baseDef, ivDef, baseSta, ivSta, optCpm)

        // Evaluate top stat products across common competitive IV combinations (e.g. 0-15 Def/Sta, 0-5 Atk)
        var betterCount = 0
        var maxStatProduct = userStatProduct

        // Representative sampling for fast evaluation
        for (a in 0..15) {
            for (d in 10..15) {
                for (s in 10..15) {
                    val (_, cpm) = findOptimalLevel(baseAtk, a, baseDef, d, baseSta, s, league.maxCp)
                    val sp = calculateStatProduct(baseAtk, a, baseDef, d, baseSta, s, cpm)
                    if (sp > maxStatProduct) {
                        maxStatProduct = sp
                    }
                    if (sp > userStatProduct) {
                        betterCount++
                    }
                }
            }
        }

        val rank = betterCount + 1
        val percent = ((userStatProduct / maxStatProduct) * 100.0).toFloat().coerceIn(80.0f, 100.0f)

        return PvpRankResult(
            league = league,
            rank = rank,
            optimalLevel = optLvl,
            cpAtLevel = cp,
            statProduct = userStatProduct,
            statProductPercent = percent
        )
    }

    fun calculateRank(instance: org.cleanroom.pokemongo.data.models.PokemonInstance, league: PvpLeague): PvpRankResult {
        return calculatePvpRank(instance.speciesId, instance.ivAttack, instance.ivDefense, instance.ivStamina, league)
    }
}
