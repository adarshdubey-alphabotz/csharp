package org.cleanroom.pokemongo.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cleanroom.pokemongo.data.models.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

enum class ThrowQuality(val displayName: String, val xpBonus: Int, val multiplier: Float) {
    NORMAL("Normal", 0, 1.0f),
    NICE("Nice!", 10, 1.15f),
    GREAT("Great!", 50, 1.30f),
    EXCELLENT("Excellent!", 100, 1.70f)
}

sealed class CatchResult {
    data class Success(
        val pokemon: PokemonInstance,
        val xpEarned: Int,
        val stardustEarned: Int,
        val candyEarned: Int,
        val throwQuality: ThrowQuality,
        val isCurveball: Boolean
    ) : CatchResult()

    data class Breakout(val escapesFlee: Boolean) : CatchResult()
    object Fled : CatchResult()
}

data class SpinResult(
    val pokestopName: String,
    val itemsAwarded: List<Pair<ItemType, Int>>,
    val xpAwarded: Int,
    val eggAwarded: EggTier?
)

class GameStateRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("cleanroom_pgo_state", Context.MODE_PRIVATE)

    // Player State
    private val _trainerProfile = MutableStateFlow(TrainerProfile())
    val trainerProfile: StateFlow<TrainerProfile> = _trainerProfile.asStateFlow()

    private val _pokemonBox = MutableStateFlow<List<PokemonInstance>>(emptyList())
    val pokemonBox: StateFlow<List<PokemonInstance>> = _pokemonBox.asStateFlow()

    private val _candies = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val candies: StateFlow<Map<Int, Int>> = _candies.asStateFlow()

    private val _items = MutableStateFlow<Map<ItemType, Int>>(emptyMap())
    val items: StateFlow<Map<ItemType, Int>> = _items.asStateFlow()

    private val _eggs = MutableStateFlow<List<Egg>>(emptyList())
    val eggs: StateFlow<List<Egg>> = _eggs.asStateFlow()

    private val _incubators = MutableStateFlow<List<Incubator>>(emptyList())
    val incubators: StateFlow<List<Incubator>> = _incubators.asStateFlow()

    private val _buddy = MutableStateFlow(Buddy())
    val buddy: StateFlow<Buddy> = _buddy.asStateFlow()

    private val _dailyStreaks = MutableStateFlow(DailyStreaks())
    val dailyStreaks: StateFlow<DailyStreaks> = _dailyStreaks.asStateFlow()

    // World & Spawns
    private val _playerLatitude = MutableStateFlow(37.7891)
    val playerLatitude: StateFlow<Double> = _playerLatitude.asStateFlow()

    private val _playerLongitude = MutableStateFlow(-122.4014)
    val playerLongitude: StateFlow<Double> = _playerLongitude.asStateFlow()

    private val _pokestops = MutableStateFlow<List<Pokestop>>(emptyList())
    val pokestops: StateFlow<List<Pokestop>> = _pokestops.asStateFlow()

    private val _gyms = MutableStateFlow<List<Gym>>(emptyList())
    val gyms: StateFlow<List<Gym>> = _gyms.asStateFlow()

    private val _wildSpawns = MutableStateFlow<List<WildPokemonSpawn>>(emptyList())
    val wildSpawns: StateFlow<List<WildPokemonSpawn>> = _wildSpawns.asStateFlow()

    private val _pendingHatchEgg = MutableStateFlow<Egg?>(null)
    val pendingHatchEgg: StateFlow<Egg?> = _pendingHatchEgg.asStateFlow()

    init {
        loadState()
        if (_pokemonBox.value.isEmpty()) {
            initStarterData()
        }
        initWorldPOIs()
        refreshWildSpawns()
    }

    private fun initStarterData() {
        val pikachu = PokemonInstance(
            speciesId = 25,
            level = 20.0f,
            ivAttack = 15,
            ivDefense = 14,
            ivStamina = 15,
            fastMove = "Thunder Shock",
            chargedMove = "Wild Charge",
            currentHp = 85,
            isFavorite = true
        )
        val charizard = PokemonInstance(
            speciesId = 6,
            level = 25.0f,
            ivAttack = 14,
            ivDefense = 12,
            ivStamina = 13,
            fastMove = "Fire Spin",
            chargedMove = "Blast Burn",
            currentHp = 140,
            isFavorite = true
        )
        val gengar = PokemonInstance(
            speciesId = 94,
            level = 22.0f,
            ivAttack = 15,
            ivDefense = 13,
            ivStamina = 11,
            fastMove = "Shadow Claw",
            chargedMove = "Shadow Ball",
            currentHp = 110
        )
        _pokemonBox.value = listOf(pikachu, charizard, gengar)

        _candies.value = mapOf(
            1 to 65,  // Bulbasaur
            4 to 80,  // Charmander
            7 to 45,  // Squirtle
            16 to 120, // Pidgey
            25 to 75, // Pikachu
            92 to 40, // Gastly
            129 to 210, // Magikarp
            133 to 95,  // Eevee
            147 to 55   // Dratini
        )

        _items.value = mapOf(
            ItemType.POKE_BALL to 45,
            ItemType.GREAT_BALL to 25,
            ItemType.ULTRA_BALL to 12,
            ItemType.POTION to 20,
            ItemType.SUPER_POTION to 15,
            ItemType.HYPER_POTION to 10,
            ItemType.MAX_POTION to 5,
            ItemType.REVIVE to 15,
            ItemType.MAX_REVIVE to 5,
            ItemType.RAZZ_BERRY to 20,
            ItemType.NANAB_BERRY to 15,
            ItemType.PINAP_BERRY to 18,
            ItemType.INCENSE to 3,
            ItemType.LUCKY_EGG to 2
        )

        _incubators.value = listOf(
            Incubator(isUnlimited = true, distanceMultiplier = 1.0f),
            Incubator(isUnlimited = false, distanceMultiplier = 0.67f, usesRemaining = 3)
        )

        _eggs.value = listOf(
            Egg(tier = EggTier.TWO_KM, walkedKm = 0.8f),
            Egg(tier = EggTier.FIVE_KM, walkedKm = 2.4f),
            Egg(tier = EggTier.TEN_KM, walkedKm = 0.0f)
        )

        _buddy.value = Buddy(
            pokemonInstanceId = pikachu.id,
            affectionKmProgress = 0.7f,
            totalKmWalked = 14.5f,
            totalCandiesFound = 14,
            hungerPercent = 85,
            emotion = BuddyEmotion.HAPPY,
            heartsWalk = 1,
            heartsFeed = 2,
            heartsPlay = 1,
            heartsBattle = 1
        )

        saveState()
    }

    private fun initWorldPOIs() {
        val lat = _playerLatitude.value
        val lon = _playerLongitude.value

        _pokestops.value = listOf(
            Pokestop(name = "Fountain Plaza PokéStop", latitude = lat + 0.0003, longitude = lon - 0.0002, hasLure = true),
            Pokestop(name = "Historic Clock Tower", latitude = lat - 0.0004, longitude = lon + 0.0003),
            Pokestop(name = "Memorial Sculpture", latitude = lat + 0.0005, longitude = lon + 0.0004),
            Pokestop(name = "Botanical Garden Gate", latitude = lat - 0.0002, longitude = lon - 0.0005)
        )

        _gyms.value = listOf(
            Gym(name = "Grand Arena Gym", latitude = lat + 0.0008, longitude = lon + 0.0001, team = Team.MYSTIC, defenderSpeciesName = "Dragonite", defenderCp = 3250),
            Gym(name = "Central Park Monument Gym", latitude = lat - 0.0006, longitude = lon - 0.0006, team = Team.VALOR, defenderSpeciesName = "Tyranitar", defenderCp = 3410)
        )
    }

    fun refreshWildSpawns() {
        val lat = _playerLatitude.value
        val lon = _playerLongitude.value
        val random = Random()

        val spawns = mutableListOf<WildPokemonSpawn>()
        for (i in 0 until 5) {
            val species = PokedexDatabase.getRandomSpawnSpecies()
            val dLat = (random.nextDouble() - 0.5) * 0.0012
            val dLon = (random.nextDouble() - 0.5) * 0.0012
            spawns.add(
                WildPokemonSpawn(
                    speciesId = species.id,
                    latitude = lat + dLat,
                    longitude = lon + dLon,
                    level = (1..25).random().toFloat()
                )
            )
        }
        _wildSpawns.value = spawns
    }

    fun updatePlayerLocation(lat: Double, lon: Double) {
        val oldLat = _playerLatitude.value
        val oldLon = _playerLongitude.value
        _playerLatitude.value = lat
        _playerLongitude.value = lon

        // Calculate approximate distance moved in km
        val dLat = Math.toRadians(lat - oldLat)
        val dLon = Math.toRadians(lon - oldLon)
        val a = Math.sin(dLat / 2).pow(2.0) +
                Math.cos(Math.toRadians(oldLat)) * Math.cos(Math.toRadians(lat)) *
                Math.sin(dLon / 2).pow(2.0)
        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
        val distanceKm = (6371.0 * c).toFloat()

        if (distanceKm > 0.005f) { // More than 5 meters
            addWalkingDistance(distanceKm)
        }
    }

    /**
     * Direct spatial teleport without accumulating physical walking distance.
     */
    fun updatePlayerLocationDirect(lat: Double, lon: Double) {
        _playerLatitude.value = lat
        _playerLongitude.value = lon
    }


    fun addWalkingDistance(distanceKm: Float) {
        val currentProfile = _trainerProfile.value
        currentProfile.totalKmWalked += distanceKm
        _trainerProfile.value = currentProfile

        // Advance incubators and eggs
        val updatedEggs = _eggs.value.toMutableList()
        val incubatorMap = _incubators.value.associateBy { it.id }

        for (egg in updatedEggs) {
            val incId = egg.incubatorId
            if (incId != null && !egg.isHatched) {
                val inc = incubatorMap[incId]
                val effectiveDistance = if (inc != null) distanceKm / inc.distanceMultiplier else distanceKm
                egg.walkedKm += effectiveDistance

                if (egg.isReadyToHatch() && _pendingHatchEgg.value == null) {
                    _pendingHatchEgg.value = egg
                }
            }
        }
        _eggs.value = updatedEggs

        // Advance buddy distance
        val currentBuddy = _buddy.value
        if (currentBuddy.pokemonInstanceId != null) {
            val buddyPokemon = _pokemonBox.value.find { it.id == currentBuddy.pokemonInstanceId }
            val species = if (buddyPokemon != null) PokedexDatabase.getById(buddyPokemon.speciesId) else null
            val targetKm = species?.buddyKmPerCandy ?: 3.0f

            currentBuddy.affectionKmProgress += distanceKm
            currentBuddy.totalKmWalked += distanceKm

            while (currentBuddy.affectionKmProgress >= targetKm) {
                currentBuddy.affectionKmProgress -= targetKm
                currentBuddy.totalCandiesFound++
                if (species != null) {
                    addCandy(species.id, 1)
                }
            }
            _buddy.value = currentBuddy
        }

        saveState()
    }

    fun catchPokemon(
        spawn: WildPokemonSpawn,
        ballType: ItemType,
        berry: ItemType?,
        throwQuality: ThrowQuality,
        isCurveball: Boolean
    ): CatchResult {
        val species = PokedexDatabase.getById(spawn.speciesId) ?: return CatchResult.Fled
        consumeItem(ballType, 1)
        if (berry != null) {
            consumeItem(berry, 1)
        }

        val baseCaptureRate = species.baseCaptureRate
        val ballMultiplier = ballType.catchMultiplier
        val berryMultiplier = berry?.catchMultiplier ?: 1.0f
        val curveMultiplier = if (isCurveball) 1.7f else 1.0f
        val throwMultiplier = throwQuality.multiplier

        // Probability calculation
        val cpm = 0.45f
        val innerProb = (baseCaptureRate / (2.0f * cpm)).coerceIn(0.01f, 0.99f)
        val totalMultiplier = (ballMultiplier * berryMultiplier * curveMultiplier * throwMultiplier).toDouble()
        val catchChance = 1.0 - (1.0 - innerProb).pow(totalMultiplier)

        val roll = Random().nextDouble()
        if (roll < catchChance) {
            // Success!
            val newPokemon = PokemonInstance(
                speciesId = species.id,
                level = spawn.level,
                ivAttack = spawn.ivAttack,
                ivDefense = spawn.ivDefense,
                ivStamina = spawn.ivStamina,
                fastMove = "Tackle",
                chargedMove = "Special Attack",
                currentHp = 100
            )
            _pokemonBox.value = _pokemonBox.value + newPokemon

            // Rewards
            val xpAward = 100 + throwQuality.xpBonus + (if (isCurveball) 10 else 0)
            val stardustAward = 100
            val candyAward = if (berry == ItemType.PINAP_BERRY) 6 else 3

            _trainerProfile.value.addXp(xpAward.toLong())
            _trainerProfile.value.stardust += stardustAward
            _trainerProfile.value.totalCatches++
            addCandy(species.id, candyAward)

            // Update daily catch streak
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val streaks = _dailyStreaks.value
            if (!streaks.isCatchCompletedToday(today)) {
                streaks.lastCatchDate = today
                streaks.catchStreak = if (streaks.catchStreak >= 7) 1 else streaks.catchStreak + 1
                _trainerProfile.value.addXp(500L)
            }
            _dailyStreaks.value = streaks

            // Remove spawn from map
            _wildSpawns.value = _wildSpawns.value.filter { it.id != spawn.id }
            saveState()

            return CatchResult.Success(
                pokemon = newPokemon,
                xpEarned = xpAward,
                stardustEarned = stardustAward,
                candyEarned = candyAward,
                throwQuality = throwQuality,
                isCurveball = isCurveball
            )
        } else {
            // Check flee rate
            val fleeRoll = Random().nextDouble()
            if (fleeRoll < species.baseFleeRate) {
                _wildSpawns.value = _wildSpawns.value.filter { it.id != spawn.id }
                return CatchResult.Fled
            }
            return CatchResult.Breakout(escapesFlee = true)
        }
    }

    fun spinPokestop(pokestopId: String): SpinResult? {
        val stop = _pokestops.value.find { it.id == pokestopId } ?: return null
        if (stop.isInCooldown()) return null

        stop.lastSpunTimestamp = System.currentTimeMillis()
        val random = Random()

        val itemsAwarded = mutableListOf<Pair<ItemType, Int>>()
        val ballsCount = random.nextInt(3) + 2
        itemsAwarded.add(Pair(ItemType.POKE_BALL, ballsCount))
        addItem(ItemType.POKE_BALL, ballsCount)

        if (random.nextBoolean()) {
            val potionCount = random.nextInt(2) + 1
            itemsAwarded.add(Pair(ItemType.POTION, potionCount))
            addItem(ItemType.POTION, potionCount)
        }

        if (random.nextInt(3) == 0) {
            itemsAwarded.add(Pair(ItemType.RAZZ_BERRY, 1))
            addItem(ItemType.RAZZ_BERRY, 1)
        }

        var eggAwarded: EggTier? = null
        if (_eggs.value.size < 9 && random.nextInt(4) == 0) {
            val tiers = listOf(EggTier.TWO_KM, EggTier.FIVE_KM, EggTier.TEN_KM)
            val selectedTier = tiers.random()
            val newEgg = Egg(tier = selectedTier)
            _eggs.value = _eggs.value + newEgg
            eggAwarded = selectedTier
        }

        var xp = 50
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val streaks = _dailyStreaks.value
        if (!streaks.isSpinCompletedToday(today)) {
            streaks.lastSpinDate = today
            streaks.spinStreak = if (streaks.spinStreak >= 7) 1 else streaks.spinStreak + 1
            xp += 500
        }
        _dailyStreaks.value = streaks

        _trainerProfile.value.addXp(xp.toLong())
        _trainerProfile.value.totalPokestopsSpun++
        saveState()

        return SpinResult(
            pokestopName = stop.name,
            itemsAwarded = itemsAwarded,
            xpAwarded = xp,
            eggAwarded = eggAwarded
        )
    }

    fun hatchEgg(eggId: String): Pair<PokemonInstance, EggTier>? {
        val egg = _eggs.value.find { it.id == eggId } ?: return null
        egg.isHatched = true

        // Free up incubator
        if (egg.incubatorId != null) {
            val inc = _incubators.value.find { it.id == egg.incubatorId }
            if (inc != null) {
                inc.currentEggId = null
                if (!inc.isUnlimited) {
                    inc.usesRemaining--
                }
            }
        }

        // Egg hatches have boosted IV floor (min 10/10/10)
        val species = PokedexDatabase.getRandomSpawnSpecies()
        val hatchedPokemon = PokemonInstance(
            speciesId = species.id,
            level = 20.0f,
            ivAttack = (10..15).random(),
            ivDefense = (10..15).random(),
            ivStamina = (10..15).random(),
            fastMove = "Tackle",
            chargedMove = "Special Attack",
            currentHp = 100
        )
        _pokemonBox.value = _pokemonBox.value + hatchedPokemon

        // Rewards
        val candyBonus = (10..22).random()
        val stardustBonus = (800..1600).random()
        addCandy(species.id, candyBonus)
        _trainerProfile.value.stardust += stardustBonus
        _trainerProfile.value.addXp(1000L)
        _trainerProfile.value.totalEggsHatched++

        _eggs.value = _eggs.value.filter { it.id != eggId }
        _pendingHatchEgg.value = null
        saveState()

        return Pair(hatchedPokemon, egg.tier)
    }

    fun placeEggInIncubator(eggId: String, incubatorId: String): Boolean {
        val egg = _eggs.value.find { it.id == eggId } ?: return false
        val inc = _incubators.value.find { it.id == incubatorId } ?: return false
        if (egg.incubatorId != null || !inc.isAvailable()) return false

        egg.incubatorId = incubatorId
        inc.currentEggId = eggId
        saveState()
        return true
    }

    fun powerUpPokemon(instanceId: String): Boolean {
        val pokemon = _pokemonBox.value.find { it.id == instanceId } ?: return false
        val species = PokedexDatabase.getById(pokemon.speciesId) ?: return false

        val stardustCost = 2500
        val candyCost = 2
        val currentCandies = _candies.value[species.id] ?: 0

        if (_trainerProfile.value.stardust < stardustCost || currentCandies < candyCost || pokemon.level >= 40.0f) {
            return false
        }

        _trainerProfile.value.stardust -= stardustCost
        addCandy(species.id, -candyCost)

        val updated = pokemon.copy(level = pokemon.level + 0.5f)
        _pokemonBox.value = _pokemonBox.value.map { if (it.id == instanceId) updated else it }
        saveState()
        return true
    }

    fun evolvePokemon(instanceId: String): PokemonInstance? {
        val pokemon = _pokemonBox.value.find { it.id == instanceId } ?: return null
        val currentSpecies = PokedexDatabase.getById(pokemon.speciesId) ?: return null
        val evolveToId = currentSpecies.evolvesToSpeciesId ?: return null
        val candyCost = currentSpecies.candyToEvolve

        val currentCandies = _candies.value[currentSpecies.id] ?: 0
        if (currentCandies < candyCost) return null

        addCandy(currentSpecies.id, -candyCost)
        _trainerProfile.value.addXp(500L)
        _trainerProfile.value.totalEvolutions++

        val evolved = pokemon.copy(
            speciesId = evolveToId,
            nickname = null
        )
        _pokemonBox.value = _pokemonBox.value.map { if (it.id == instanceId) evolved else it }
        saveState()
        return evolved
    }

    fun transferPokemon(instanceId: String): Boolean {
        val pokemon = _pokemonBox.value.find { it.id == instanceId } ?: return false
        if (pokemon.isFavorite) return false

        _pokemonBox.value = _pokemonBox.value.filter { it.id != instanceId }
        addCandy(pokemon.speciesId, 1)
        saveState()
        return true
    }

    fun toggleFavorite(instanceId: String) {
        val updated = _pokemonBox.value.map {
            if (it.id == instanceId) it.copy(isFavorite = !it.isFavorite) else it
        }
        _pokemonBox.value = updated
        saveState()
    }

    fun healPokemon(instanceId: String, itemType: ItemType): Boolean {
        val pokemon = _pokemonBox.value.find { it.id == instanceId } ?: return false
        val species = PokedexDatabase.getById(pokemon.speciesId) ?: return false
        val maxHp = pokemon.calculateMaxHp(species)

        if (pokemon.currentHp <= 0 || pokemon.currentHp >= maxHp) return false
        if ((_items.value[itemType] ?: 0) <= 0) return false

        consumeItem(itemType, 1)
        val newHp = (pokemon.currentHp + itemType.healAmount).coerceAtMost(maxHp)
        _pokemonBox.value = _pokemonBox.value.map { if (it.id == instanceId) it.copy(currentHp = newHp) else it }
        saveState()
        return true
    }

    fun revivePokemon(instanceId: String, itemType: ItemType): Boolean {
        val pokemon = _pokemonBox.value.find { it.id == instanceId } ?: return false
        val species = PokedexDatabase.getById(pokemon.speciesId) ?: return false
        val maxHp = pokemon.calculateMaxHp(species)

        if (pokemon.currentHp > 0) return false
        if ((_items.value[itemType] ?: 0) <= 0) return false

        consumeItem(itemType, 1)
        val newHp = if (itemType == ItemType.MAX_REVIVE) maxHp else maxHp / 2
        _pokemonBox.value = _pokemonBox.value.map { if (it.id == instanceId) it.copy(currentHp = newHp) else it }
        saveState()
        return true
    }

    fun assignBuddy(instanceId: String) {
        _buddy.value = _buddy.value.copy(
            pokemonInstanceId = instanceId,
            affectionKmProgress = 0.0f
        )
        saveState()
    }

    fun feedBuddy(berryType: ItemType): Boolean {
        if ((_items.value[berryType] ?: 0) <= 0) return false
        consumeItem(berryType, 1)

        val current = _buddy.value
        current.hungerPercent = (current.hungerPercent + 34).coerceAtMost(100)
        current.emotion = when {
            current.hungerPercent >= 90 -> BuddyEmotion.EXCITED
            current.hungerPercent >= 60 -> BuddyEmotion.HAPPY
            current.hungerPercent >= 30 -> BuddyEmotion.SMILE
            else -> BuddyEmotion.NORMAL
        }
        if (current.heartsFeed < 3) {
            current.heartsFeed++
        }
        _buddy.value = current
        saveState()
        return true
    }

    fun playWithBuddy(): Boolean {
        val current = _buddy.value
        if (current.heartsPlay < 1) {
            current.heartsPlay++
            current.emotion = BuddyEmotion.HAPPY
            _buddy.value = current
            saveState()
            return true
        }
        return false
    }

    private fun addCandy(speciesId: Int, amount: Int) {
        val current = _candies.value.toMutableMap()
        val count = (current[speciesId] ?: 0) + amount
        current[speciesId] = count.coerceAtLeast(0)
        _candies.value = current
    }

    private fun addItem(itemType: ItemType, count: Int) {
        val current = _items.value.toMutableMap()
        current[itemType] = (current[itemType] ?: 0) + count
        _items.value = current
    }

    private fun consumeItem(itemType: ItemType, count: Int) {
        val current = _items.value.toMutableMap()
        val remaining = (current[itemType] ?: 0) - count
        current[itemType] = remaining.coerceAtLeast(0)
        _items.value = current
    }

    private fun saveState() {
        val editor = prefs.edit()
        val trainer = _trainerProfile.value
        editor.putString("trainer_name", trainer.name)
        editor.putInt("trainer_level", trainer.level)
        editor.putLong("trainer_xp", trainer.currentXp)
        editor.putInt("trainer_stardust", trainer.stardust)
        editor.putInt("trainer_coins", trainer.pokeCoins)
        editor.putString("trainer_team", trainer.team.name)
        editor.putFloat("trainer_km", trainer.totalKmWalked)
        editor.putInt("trainer_catches", trainer.totalCatches)
        editor.putInt("trainer_stops", trainer.totalPokestopsSpun)
        editor.putInt("trainer_hatches", trainer.totalEggsHatched)
        editor.putInt("trainer_evolutions", trainer.totalEvolutions)

        // Save Pokemon Box
        val boxArray = JSONArray()
        for (p in _pokemonBox.value) {
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("speciesId", p.speciesId)
            obj.put("nickname", p.nickname ?: "")
            obj.put("level", p.level.toDouble())
            obj.put("ivAttack", p.ivAttack)
            obj.put("ivDefense", p.ivDefense)
            obj.put("ivStamina", p.ivStamina)
            obj.put("fastMove", p.fastMove)
            obj.put("chargedMove", p.chargedMove)
            obj.put("currentHp", p.currentHp)
            obj.put("isFavorite", p.isFavorite)
            boxArray.put(obj)
        }
        editor.putString("pokemon_box_json", boxArray.toString())
        editor.apply()
    }

    private fun loadState() {
        if (!prefs.contains("trainer_name")) return

        val profile = TrainerProfile(
            name = prefs.getString("trainer_name", "Red") ?: "Red",
            level = prefs.getInt("trainer_level", 28),
            currentXp = prefs.getLong("trainer_xp", 185000L),
            stardust = prefs.getInt("trainer_stardust", 145000),
            pokeCoins = prefs.getInt("trainer_coins", 420),
            team = Team.valueOf(prefs.getString("trainer_team", "MYSTIC") ?: "MYSTIC"),
            totalKmWalked = prefs.getFloat("trainer_km", 248.5f),
            totalCatches = prefs.getInt("trainer_catches", 1240),
            totalPokestopsSpun = prefs.getInt("trainer_stops", 890),
            totalEggsHatched = prefs.getInt("trainer_hatches", 64),
            totalEvolutions = prefs.getInt("trainer_evolutions", 112)
        )
        _trainerProfile.value = profile

        val boxJson = prefs.getString("pokemon_box_json", null)
        if (boxJson != null) {
            try {
                val boxArray = JSONArray(boxJson)
                val list = mutableListOf<PokemonInstance>()
                for (i in 0 until boxArray.length()) {
                    val obj = boxArray.getJSONObject(i)
                    list.add(
                        PokemonInstance(
                            id = obj.getString("id"),
                            speciesId = obj.getInt("speciesId"),
                            nickname = obj.optString("nickname").ifEmpty { null },
                            level = obj.optDouble("level", 20.0).toFloat(),
                            ivAttack = obj.getInt("ivAttack"),
                            ivDefense = obj.getInt("ivDefense"),
                            ivStamina = obj.getInt("ivStamina"),
                            fastMove = obj.getString("fastMove"),
                            chargedMove = obj.getString("chargedMove"),
                            currentHp = obj.getInt("currentHp"),
                            isFavorite = obj.optBoolean("isFavorite", false)
                        )
                    )
                }
                _pokemonBox.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        @Volatile
        private var instance: GameStateRepository? = null

        fun getInstance(context: Context): GameStateRepository {
            return instance ?: synchronized(this) {
                instance ?: GameStateRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
