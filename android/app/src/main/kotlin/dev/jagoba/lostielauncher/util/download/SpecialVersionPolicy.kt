package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.SpecialVersionConfig
import java.util.UUID

object SpecialVersionPolicy {
    private val keyPattern = Regex("^[A-Za-z0-9]{4}(-[A-Za-z0-9]{4}){4}$")
    private val archivePattern = Regex("^[A-Za-z0-9._-]+\\.zip$")
    private val hashPattern = Regex("^[A-Fa-f0-9]{64}$")

    fun isValidKey(key: String): Boolean = keyPattern.matches(key)

    fun isValidConfig(config: SpecialVersionConfig): Boolean =
        archivePattern.matches(config.fileName) && hashPattern.matches(config.sha256) && config.version.isNotBlank()

    fun matchesGame(config: SpecialVersionConfig, gameId: UUID?): Boolean =
        gameId != null && config.mainGameId == gameId
}
