package org.cleanroom.pokemongo.data.account

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.cleanroom.pokemongo.data.models.Team
import org.cleanroom.pokemongo.data.models.TrainerProfile
import org.cleanroom.pokemongo.data.repository.GameStateRepository
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AccountProfile(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val level: Int = 1,
    val team: Team = Team.MYSTIC,
    val totalCatches: Int = 0,
    val stardust: Int = 1000,
    val isCurrent: Boolean = false
)

class AccountManager private constructor(
    private val context: Context,
    private val repo: GameStateRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cleanroom_accounts_prefs", Context.MODE_PRIVATE)

    private val _accounts = MutableStateFlow<List<AccountProfile>>(loadAccounts())
    val accounts: StateFlow<List<AccountProfile>> = _accounts.asStateFlow()

    fun switchAccount(accountId: String): Boolean {
        val target = _accounts.value.find { it.id == accountId } ?: return false

        val updated = _accounts.value.map {
            it.copy(isCurrent = it.id == accountId)
        }
        _accounts.value = updated
        saveAccounts(updated)

        // Sync with GameStateRepository TrainerProfile
        val currentProfile = repo.trainerProfile.value
        currentProfile.name = target.username
        currentProfile.level = target.level
        currentProfile.team = target.team
        currentProfile.stardust = target.stardust
        currentProfile.totalCatches = target.totalCatches
        return true
    }

    fun addAccount(username: String, team: Team): AccountProfile {
        val newAcc = AccountProfile(
            username = username,
            team = team,
            level = 1,
            stardust = 5000,
            isCurrent = false
        )
        val updated = _accounts.value + newAcc
        _accounts.value = updated
        saveAccounts(updated)
        return newAcc
    }

    fun removeAccount(accountId: String): Boolean {
        if (_accounts.value.size <= 1) return false // Keep at least one account
        val target = _accounts.value.find { it.id == accountId } ?: return false
        if (target.isCurrent) {
            val fallback = _accounts.value.first { it.id != accountId }
            switchAccount(fallback.id)
        }
        val updated = _accounts.value.filter { it.id != accountId }
        _accounts.value = updated
        saveAccounts(updated)
        return true
    }

    private fun saveAccounts(list: List<AccountProfile>) {
        val arr = JSONArray()
        for (acc in list) {
            val obj = JSONObject()
            obj.put("id", acc.id)
            obj.put("username", acc.username)
            obj.put("level", acc.level)
            obj.put("team", acc.team.name)
            obj.put("catches", acc.totalCatches)
            obj.put("stardust", acc.stardust)
            obj.put("isCurrent", acc.isCurrent)
            arr.put(obj)
        }
        prefs.edit().putString("saved_accounts_json", arr.toString()).apply()
    }

    private fun loadAccounts(): List<AccountProfile> {
        val raw = prefs.getString("saved_accounts_json", null)
        if (raw.isNullOrEmpty()) {
            return listOf(
                AccountProfile(
                    username = "AshKetchum",
                    level = 38,
                    team = Team.VALOR,
                    totalCatches = 1420,
                    stardust = 245000,
                    isCurrent = true
                ),
                AccountProfile(
                    username = "MistyWaterflower",
                    level = 32,
                    team = Team.MYSTIC,
                    totalCatches = 890,
                    stardust = 120000,
                    isCurrent = false
                )
            )
        }

        val list = mutableListOf<AccountProfile>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    AccountProfile(
                        id = obj.getString("id"),
                        username = obj.getString("username"),
                        level = obj.getInt("level"),
                        team = Team.valueOf(obj.getString("team")),
                        totalCatches = obj.getInt("catches"),
                        stardust = obj.getInt("stardust"),
                        isCurrent = obj.getBoolean("isCurrent")
                    )
                )
            }
        } catch (e: Exception) {
            list.clear()
        }
        return if (list.isNotEmpty()) list else listOf(
            AccountProfile(username = "AshKetchum", level = 38, team = Team.VALOR, isCurrent = true)
        )
    }

    companion object {
        @Volatile
        private var instance: AccountManager? = null

        fun getInstance(context: Context, repo: GameStateRepository): AccountManager {
            return instance ?: synchronized(this) {
                instance ?: AccountManager(context.applicationContext, repo).also { instance = it }
            }
        }
    }
}
