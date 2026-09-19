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

/**
 * A timestamp as the CDN writes it, kept in both of the forms the launcher
 * needs.
 *
 * This is the single most misreadable thing in the remote format, so it is a
 * type rather than a string. The payload writes `yyyy-MM-ddTHH:mm:ss` with **no
 * offset**, and those are Central European wall-clock times written by the
 * publisher. They are therefore *not* to be compared against the device's local
 * time, and not against UTC: an item published from Madrid expires when Madrid
 * says it does, whatever zone the phone is in.
 *
 * The desktop reaches the same rule the long way round, through
 * `DateTime.Kind`: UTC-kind converts from UTC, local-kind converts from the
 * machine zone, and unspecified-kind — what the real payload always produces —
 * is taken as already being CET.
 *
 * @property asWritten the value exactly as published, which is what the UI
 *   renders. The desktop renders its raw `DateTime` the same way.
 * @property inCet the same moment expressed in Central European Time, which is
 *   what expiry is compared against.
 */
@Serializable(with = CdnDateTimeSerializer::class)
internal data class CdnDateTime(val asWritten: LocalDateTime, val inCet: LocalDateTime) {
    companion object {
        /**
         * Central European Time.
         *
         * The desktop asks Windows for `Central Europe Standard Time` and falls
         * back to UTC if the machine has no such zone — a fallback
         * `spec/04-data-model.md` calls a documented compromise rather than
         * something to reproduce. Android ships the IANA database with the
         * platform, so the zone always resolves and there is nothing to fall
         * back to. `Europe/Madrid` is CET/CEST and is the publisher's own zone.
         */
        val CET: ZoneId = ZoneId.of("Europe/Madrid")

        /**
         * What an absent `date` becomes.
         *
         * The field is never absent in practice, but the desktop tolerates its
         * absence — `System.Text.Json` leaves the struct at `DateTime.MinValue`
         * — and failing a whole payload where the desktop shows one oddly dated
         * item would be a behaviour change, not a fix.
         */
        val MIN_VALUE: CdnDateTime = LocalDateTime.of(1, 1, 1, 0, 0).let { CdnDateTime(it, it) }

        /**
         * Parses a published timestamp.
         *
         * @throws DateTimeParseException if the text is neither an offset-bearing
         *   ISO timestamp nor a local one.
         */
        fun parse(text: String): CdnDateTime {
            val offset = runCatching { OffsetDateTime.parse(text) }.getOrNull()
            if (offset != null) {
                // An offset was published, so the instant is unambiguous: state
                // it in CET. This covers both of the desktop's converting
                // branches, since converting via the machine zone and
                // converting directly preserve the instant.
                val inCet = offset.atZoneSameInstant(CET).toLocalDateTime()
                return CdnDateTime(asWritten = offset.toLocalDateTime(), inCet = inCet)
            }

            // No offset: already CET, by the publisher's convention.
            val local = LocalDateTime.parse(text)
            return CdnDateTime(asWritten = local, inCet = local)
        }
    }
}

/**
 * Reads a [CdnDateTime] from a JSON string.
 *
 * A malformed timestamp fails the whole payload rather than being coerced or
 * skipped: on the desktop the same text fails `System.Text.Json`, and the
 * content service's response to a payload it cannot read is to keep the last
 * good one and flag it stale. Silently dropping a date would turn that into a
 * cache replaced by garbage.
 */
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

    override fun serialize(encoder: Encoder, value: CdnDateTime) {
        // The launcher only ever reads this payload; writing it back is not a
        // scenario, and guessing which of the two forms to emit would be one.
        throw SerializationException("A CDN timestamp is read-only.")
    }
}
