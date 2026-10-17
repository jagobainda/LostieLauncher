package dev.jagoba.lostielauncher.service.cdn.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class GameDto(
    val id: String = "",
    val nombre: String = "",
    val version: String = "",
    val pesoGB: Double = 0.0,
    val descripcion: String = "",
    val url: String = "",
    val logo: String = "",
    val rutaRelativa: String = "",
    val sha256: String = "",
)
