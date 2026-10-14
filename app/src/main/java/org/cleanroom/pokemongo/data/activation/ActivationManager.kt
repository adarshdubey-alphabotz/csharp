package org.cleanroom.pokemongo.data.activation

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface ActivationValidator {
    fun validateKey(key: String, currentTime: Long = System.currentTimeMillis()): Result<ActivationState.Active>
}

class LocalDevActivationValidator : ActivationValidator {
    override fun validateKey(key: String, currentTime: Long): Result<ActivationState.Active> {
        val cleanKey = key.trim().uppercase().replace(Regex("[ _]+"), "-")
        return when {
            cleanKey.isEmpty() -> Result.failure(IllegalArgumentException("Activation key cannot be empty"))
            cleanKey == "DEV-PRO-2026" -> {
                val expiry = currentTime + (365L * 24 * 60 * 60 * 1000L) // 1 year
                Result.success(ActivationState.Active(cleanKey, expiry, ActivationTier.PRO))
            }
            cleanKey == "TRIAL-7DAYS" -> {
                val expiry = currentTime + (7L * 24 * 60 * 60 * 1000L) // 7 days
                Result.success(ActivationState.Active(cleanKey, expiry, ActivationTier.PRO))
            }
            cleanKey == "LIFETIME-PRO" -> {
                Result.success(ActivationState.Active(cleanKey, 0L, ActivationTier.PRO)) // Lifetime (no expiration)
            }
            cleanKey.matches(Regex("^PRO-[A-Z0-9]{4}-[A-Z0-9]{4}$")) -> {
                val expiry = currentTime + (30L * 24 * 60 * 60 * 1000L) // 30 days
                Result.success(ActivationState.Active(cleanKey, expiry, ActivationTier.PRO))
            }
            else -> Result.failure(IllegalArgumentException("Invalid activation key format. Accepted test keys: DEV-PRO-2026, TRIAL-7DAYS, LIFETIME-PRO, or PRO-XXXX-XXXX"))
        }
    }
}

class ActivationManager(
    context: Context,
    private val validator: ActivationValidator = LocalDevActivationValidator()
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("cleanroom_activation_prefs", Context.MODE_PRIVATE)

    private val _activationState = MutableStateFlow<ActivationState>(ActivationState.Inactive())
    val activationState: StateFlow<ActivationState> = _activationState.asStateFlow()

    init {
        loadActivationState()
    }

    fun activate(key: String): Result<ActivationState.Active> {
        val result = validator.validateKey(key)
        if (result.isSuccess) {
            val activeState = result.getOrThrow()
            _activationState.value = activeState
            saveActivationState(activeState)
        }
        return result
    }

    fun deactivate() {
        _activationState.value = ActivationState.Inactive("User deactivated license")
        prefs.edit().clear().apply()
    }

    fun isFeatureUnlocked(feature: ProFeature): Boolean {
        val state = _activationState.value
        return when (state) {
            is ActivationState.Active -> !state.isExpired() && (state.tier == ActivationTier.PRO || feature.ordinal >= 0)
            is ActivationState.Inactive -> false
        }
    }

    fun isProActive(): Boolean {
        val state = _activationState.value
        return state is ActivationState.Active && !state.isExpired()
    }

    private fun saveActivationState(state: ActivationState.Active) {
        prefs.edit()
            .putString(KEY_LICENSE, state.licenseKey)
            .putLong(KEY_EXPIRY, state.expiryTimestamp)
            .putString(KEY_TIER, state.tier.name)
            .apply()
    }

    private fun loadActivationState() {
        val key = prefs.getString(KEY_LICENSE, null)
        val expiry = prefs.getLong(KEY_EXPIRY, -1L)
        val tierStr = prefs.getString(KEY_TIER, null)

        if (key != null && tierStr != null) {
            val tier = try { ActivationTier.valueOf(tierStr) } catch (e: Exception) { ActivationTier.FREE }
            val state = ActivationState.Active(key, expiry, tier)
            if (!state.isExpired()) {
                _activationState.value = state
            } else {
                _activationState.value = ActivationState.Inactive("License expired on " + java.util.Date(expiry))
            }
        } else {
            _activationState.value = ActivationState.Inactive()
        }
    }

    companion object {
        private const val KEY_LICENSE = "license_key"
        private const val KEY_EXPIRY = "license_expiry"
        private const val KEY_TIER = "license_tier"

        @Volatile
        private var instance: ActivationManager? = null

        fun getInstance(context: Context): ActivationManager {
            return instance ?: synchronized(this) {
                instance ?: ActivationManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
