package moe.nea.ledger.analysis

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

@Serializable
data class Visualization(
	val label: String,
	val xLabel: String,
	val yLabel: String,
	val dataPoints: List<DataPoint>
)

@Serializable
data class DataPoint(
	val time: @Serializable(InstantSerializer::class) Instant,
	val value: Double,
)

object InstantSerializer : KSerializer<Instant> {
	override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("java.time.Instant", PrimitiveKind.LONG)
	override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeLong(value.toEpochMilli())
	override fun deserialize(decoder: Decoder): Instant = Instant.ofEpochMilli(decoder.decodeLong())
}