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
