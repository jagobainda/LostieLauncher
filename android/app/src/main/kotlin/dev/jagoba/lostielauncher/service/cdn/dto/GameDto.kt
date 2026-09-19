package dev.jagoba.lostielauncher.service.cdn.dto

import kotlinx.serialization.Serializable

/**
 * One element of `listado.json`, exactly as the CDN writes it.
 *
 * The property names are the wire names — Spanish, camel-cased — and they are
 * matched literally, not by a naming convention. The domain model this maps to
 * is [dev.jagoba.lostielauncher.model.GameInfo]; the mapping is in
 * `CdnMappers.kt`.
 *
 * Two nullability details carry behaviour, and both reproduce the desktop:
 *
 * - **A missing field is not the same as an explicit `null`.** Every field has
 *   a default, so an omitted `nombre` yields an empty name and the catalogue
 *   still loads; an explicit `"nombre": null` fails deserialization, the
 *   service logs it and degrades to an empty catalogue. The desktop gets the
 *   same split from `RespectNullableAnnotations`. This only holds while the
 *   `Json` instance leaves `coerceInputValues` off — turning it on would
 *   silently accept a null name.
 * - **[id] is a non-nullable string here, not a UUID.** Non-nullable so that
 *   an explicit `"id": null` fails the payload exactly as it does on the
 *   desktop; a string so that an absent id can arrive as `""` and the mapper
 *   can decide what "no id" means.
 */
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
