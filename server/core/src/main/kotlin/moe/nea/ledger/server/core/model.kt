@file:UseSerializers(UUIDSerializer::class)

package moe.nea.ledger.server.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import java.util.UUID

object UUIDSerializer : KSerializer<UUID> {
	override val descriptor: SerialDescriptor
		get() = PrimitiveSerialDescriptor("LedgerUUID", PrimitiveKind.STRING)

	override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): UUID {
		return UUID.fromString(decoder.decodeString())
	}

	override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: UUID) {
		encoder.encodeString(value.toString())
	}
}

@Serializable
data class Profile(
	val playerId: UUID,
	val profileId: UUID,
)