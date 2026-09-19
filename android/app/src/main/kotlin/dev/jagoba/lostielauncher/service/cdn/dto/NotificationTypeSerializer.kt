package dev.jagoba.lostielauncher.service.cdn.dto

import dev.jagoba.lostielauncher.model.NotificationType
import java.util.Locale
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Reads a [NotificationType] from its wire name.
 *
 * The wire spells the .NET member names — `Info`, `Warning`, `Exclamation` —
 * while the Kotlin members are `INFO`, `WARNING`, `EXCLAMATION`, so a plain
 * `@SerialName` per member would work but would leave the two spellings to
 * drift apart. Matching on the name case-insensitively also mirrors
 * `JsonStringEnumConverter`, which accepts any casing.
 *
 * An unknown value is an error, not a fallback to `Info`: the desktop fails the
 * payload there too, and a notification whose severity the launcher does not
 * understand is better not shown than shown as harmless.
 */
internal object NotificationTypeSerializer : KSerializer<NotificationType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("dev.jagoba.lostielauncher.NotificationType", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): NotificationType {
        val name = decoder.decodeString()
        return NotificationType.entries.firstOrNull { it.name == name.uppercase(Locale.ROOT) }
            ?: throw SerializationException("Unknown notification type: '$name'.")
    }

    override fun serialize(encoder: Encoder, value: NotificationType) {
        encoder.encodeString(value.name.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase(Locale.ROOT) })
    }
}
