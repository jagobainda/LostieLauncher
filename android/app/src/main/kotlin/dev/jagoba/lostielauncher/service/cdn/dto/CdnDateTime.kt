package dev.jagoba.lostielauncher.service.cdn.dto

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = CdnDateTimeSerializer::class)
internal data class CdnDateTime(val asWritten: LocalDateTime, val inCet: LocalDateTime) {
    companion object {
        val CET: ZoneId = ZoneId.of("Europe/Madrid")

        val MIN_VALUE: CdnDateTime = LocalDateTime.of(1, 1, 1, 0, 0).let { CdnDateTime(it, it) }

        fun parse(text: String): CdnDateTime {
            val offset = runCatching { OffsetDateTime.parse(text) }.getOrNull()
            if (offset != null) {
                val inCet = offset.atZoneSameInstant(CET).toLocalDateTime()
                return CdnDateTime(asWritten = offset.toLocalDateTime(), inCet = inCet)
            }

            val local = LocalDateTime.parse(text)
            return CdnDateTime(asWritten = local, inCet = local)
        }
    }
}

internal object CdnDateTimeSerializer : KSerializer<CdnDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("dev.jagoba.lostielauncher.CdnDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): CdnDateTime {
        val text = decoder.decodeString()
        return try {
            CdnDateTime.parse(text)
        } catch (e: DateTimeParseException) {
            throw SerializationException("Not a CDN timestamp: '$text'.", e)
        }
    }

    override fun serialize(encoder: Encoder, value: CdnDateTime): Unit =
        throw SerializationException("A CDN timestamp is read-only.")
}
