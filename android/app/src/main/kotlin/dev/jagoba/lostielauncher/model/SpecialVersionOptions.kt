package dev.jagoba.lostielauncher.model

data class SpecialVersionOptions(
    val baseUrl: String,
    val configFileName: String,
    val maxConfigBytes: Long = DEFAULT_MAX_CONFIG_BYTES,
) {
    companion object {
        const val DEFAULT_MAX_CONFIG_BYTES: Long = 64L * 1024
    }
}
