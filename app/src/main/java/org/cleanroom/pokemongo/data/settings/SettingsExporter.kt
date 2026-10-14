package org.cleanroom.pokemongo.data.settings

import org.cleanroom.pokemongo.data.automation.AutomationConfig
import org.cleanroom.pokemongo.data.models.ItemType

object SettingsExporter {

    fun exportToJson(config: AutomationConfig): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"version\": \"1.0.0\",\n")
        sb.append("  \"exportTimestamp\": ${System.currentTimeMillis()},\n")
        sb.append("  \"config\": {\n")
        sb.append("    \"autoCatchEnabled\": ${config.autoCatchEnabled},\n")
        sb.append("    \"autoSpinEnabled\": ${config.autoSpinEnabled},\n")
        sb.append("    \"autoFeedEnabled\": ${config.autoFeedEnabled},\n")
        sb.append("    \"preferredBall\": \"${config.preferredBall.name}\",\n")
        sb.append("    \"enhancedThrowEnabled\": ${config.enhancedThrowEnabled},\n")
        sb.append("    \"enhancedThrowQuality\": \"${config.enhancedThrowQuality.name}\",\n")
        sb.append("    \"guaranteedCurveball\": ${config.guaranteedCurveball},\n")
        sb.append("    \"arPlusBonusEnabled\": ${config.arPlusBonusEnabled},\n")
        sb.append("    \"encounterIvEnabled\": ${config.encounterIvEnabled},\n")
        sb.append("    \"caughtPreviewEnabled\": ${config.caughtPreviewEnabled},\n")
        sb.append("    \"inventoryIvBadgesEnabled\": ${config.inventoryIvBadgesEnabled},\n")
        sb.append("    \"joystickEnabled\": ${config.joystickEnabled},\n")
        sb.append("    \"speedControlsEnabled\": ${config.speedControlsEnabled},\n")
        sb.append("    \"blockNonShinyEnabled\": ${config.blockNonShinyEnabled},\n")
        sb.append("    \"blockNonHundoEnabled\": ${config.blockNonHundoEnabled},\n")
        sb.append("    \"quickCatchEnabled\": ${config.quickCatchEnabled},\n")
        sb.append("    \"autoDiscardEnabled\": ${config.autoDiscardEnabled},\n")
        sb.append("    \"transferOnCatchEnabled\": ${config.transferOnCatchEnabled},\n")
        sb.append("    \"transferIvThreshold\": ${config.transferIvThreshold},\n")
        sb.append("    \"keepShinyOnCatch\": ${config.keepShinyOnCatch},\n")
        sb.append("    \"keepHundoOnCatch\": ${config.keepHundoOnCatch},\n")
        sb.append("    \"keepLegendaryOnCatch\": ${config.keepLegendaryOnCatch},\n")
        sb.append("    \"keepPvPRankOnCatch\": ${config.keepPvPRankOnCatch},\n")
        sb.append("    \"autoIncubatorEnabled\": ${config.autoIncubatorEnabled},\n")
        sb.append("    \"eggIncubatorPriority\": \"${config.eggIncubatorPriority}\",\n")
        sb.append("    \"giftAssistantEnabled\": ${config.giftAssistantEnabled},\n")
        sb.append("    \"questAssistantEnabled\": ${config.questAssistantEnabled},\n")
        sb.append("    \"spawnBoosterMultiplier\": ${config.spawnBoosterMultiplier},\n")
        sb.append("    \"hideTrainerName\": ${config.hideTrainerName},\n")
        sb.append("    \"hideAppWithGestureEnabled\": ${config.hideAppWithGestureEnabled},\n")
        sb.append("    \"customShortcuts\": \"${config.customShortcuts.joinToString(",")}\",\n")
        sb.append("    \"appLanguage\": \"${config.appLanguage}\",\n")
        sb.append("    \"pokemonFeedLanguage\": \"${config.pokemonFeedLanguage}\"\n")
        sb.append("  }\n")
        sb.append("}")
        return sb.toString()
    }

    fun importFromJson(jsonString: String): Result<AutomationConfig> {
        return try {
            fun parseBool(key: String, default: Boolean): Boolean {
                val match = Regex("\"$key\"\\s*:\\s*(true|false)").find(jsonString)
                return match?.groupValues?.get(1)?.toBoolean() ?: default
            }
            fun parseFloat(key: String, default: Float): Float {
                val match = Regex("\"$key\"\\s*:\\s*([0-9.]+)").find(jsonString)
                return match?.groupValues?.get(1)?.toFloatOrNull() ?: default
            }
            fun parseInt(key: String, default: Int): Int {
                val match = Regex("\"$key\"\\s*:\\s*([0-9]+)").find(jsonString)
                return match?.groupValues?.get(1)?.toIntOrNull() ?: default
            }
            fun parseString(key: String, default: String): String {
                val match = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"").find(jsonString)
                return match?.groupValues?.get(1) ?: default
            }

            val ballName = parseString("preferredBall", ItemType.POKE_BALL.name)
            val ball = try { ItemType.valueOf(ballName) } catch (e: Exception) { ItemType.POKE_BALL }

            val qualityName = parseString("enhancedThrowQuality", org.cleanroom.pokemongo.data.repository.ThrowQuality.EXCELLENT.name)
            val throwQuality = try { org.cleanroom.pokemongo.data.repository.ThrowQuality.valueOf(qualityName) } catch (e: Exception) { org.cleanroom.pokemongo.data.repository.ThrowQuality.EXCELLENT }

            val shortcutsRaw = parseString("customShortcuts", "SNIPER,ROUTES,TOOLS,HEAL,SPAWNS")
            val shortcuts = shortcutsRaw.split(",").filter { it.isNotBlank() }

            val config = AutomationConfig(
                autoCatchEnabled = parseBool("autoCatchEnabled", false),
                autoSpinEnabled = parseBool("autoSpinEnabled", false),
                autoFeedEnabled = parseBool("autoFeedEnabled", false),
                preferredBall = ball,
                enhancedThrowEnabled = parseBool("enhancedThrowEnabled", false),
                enhancedThrowQuality = throwQuality,
                guaranteedCurveball = parseBool("guaranteedCurveball", true),
                arPlusBonusEnabled = parseBool("arPlusBonusEnabled", false),
                encounterIvEnabled = parseBool("encounterIvEnabled", true),
                caughtPreviewEnabled = parseBool("caughtPreviewEnabled", true),
                inventoryIvBadgesEnabled = parseBool("inventoryIvBadgesEnabled", true),
                joystickEnabled = parseBool("joystickEnabled", true),
                speedControlsEnabled = parseBool("speedControlsEnabled", true),
                blockNonShinyEnabled = parseBool("blockNonShinyEnabled", false),
                blockNonHundoEnabled = parseBool("blockNonHundoEnabled", false),
                quickCatchEnabled = parseBool("quickCatchEnabled", false),
                autoDiscardEnabled = parseBool("autoDiscardEnabled", false),
                transferOnCatchEnabled = parseBool("transferOnCatchEnabled", false),
                transferIvThreshold = parseFloat("transferIvThreshold", 90.0f),
                keepShinyOnCatch = parseBool("keepShinyOnCatch", true),
                keepHundoOnCatch = parseBool("keepHundoOnCatch", true),
                keepLegendaryOnCatch = parseBool("keepLegendaryOnCatch", true),
                keepPvPRankOnCatch = parseBool("keepPvPRankOnCatch", true),
                autoIncubatorEnabled = parseBool("autoIncubatorEnabled", false),
                eggIncubatorPriority = parseString("eggIncubatorPriority", "SHORTEST_FIRST"),
                giftAssistantEnabled = parseBool("giftAssistantEnabled", false),
                questAssistantEnabled = parseBool("questAssistantEnabled", false),
                spawnBoosterMultiplier = parseInt("spawnBoosterMultiplier", 1),
                hideTrainerName = parseBool("hideTrainerName", false),
                hideAppWithGestureEnabled = parseBool("hideAppWithGestureEnabled", true),
                customShortcuts = if (shortcuts.isNotEmpty()) shortcuts else listOf("SNIPER", "ROUTES", "TOOLS", "HEAL", "SPAWNS"),
                appLanguage = parseString("appLanguage", "English"),
                pokemonFeedLanguage = parseString("pokemonFeedLanguage", "English")
            )
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Failed to parse settings JSON: ${e.message}"))
        }
    }
}
