package moe.nea.ledger.server.core.api

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import sh.ondr.jsonschema.JsonSchema

@Serializable
data class OpenApiModel(
	val openapi: String = "3.0.0",
	val info: Info,
	val servers: List<Server>,
	val paths: Map<OpenApiPath, OpenApiRoute>,
)

@Serializable // TODO: custom serializer
@JvmInline
value class OpenApiPath(val name: String)

@Serializable
data class OpenApiRoute(
	val summary: String,
	val description: String,
	val get: OpenApiOperation?,
	val patch: OpenApiOperation?,
	val post: OpenApiOperation?,
	val delete: OpenApiOperation?,
)

@Serializable
data class OpenApiOperation(
	val tags: List<Tag>,
	val summary: String,
	val description: String,
	val operationId: String,
	val deprecated: Boolean,
//	val parameters: List<Parameter>,
	val responses: Map<@Serializable(HttpStatusCodeIntAsString::class) HttpStatusCode, OpenApiResponse>
)

object HttpStatusCodeIntAsString : KSerializer<HttpStatusCode> {
	override val descriptor: SerialDescriptor =
		PrimitiveSerialDescriptor("HttpStatusCodeIntAsString", PrimitiveKind.STRING)

	override fun deserialize(decoder: Decoder): HttpStatusCode {
		return HttpStatusCode.fromValue(decoder.decodeString().toInt())
	}

	override fun serialize(encoder: Encoder, value: HttpStatusCode) {
		encoder.encodeString(value.value.toString())
	}
}

object ContentTypeSerializer : KSerializer<ContentType> {
	override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ContentTypeSerializer", PrimitiveKind.STRING)

	override fun deserialize(decoder: Decoder): ContentType {
		return ContentType.parse(decoder.decodeString())
	}

	override fun serialize(encoder: Encoder, value: ContentType) {
		encoder.encodeString(value.contentType + "/" + value.contentSubtype)
	}
}

@Serializable
data class OpenApiResponse(
	val description: String,
	val content: Map<@Serializable(ContentTypeSerializer::class) ContentType, OpenApiResponseContentType>
)

@Serializable
data class OpenApiResponseContentType(
	val schema: JsonSchema?
)

@Serializable
@JvmInline
value class Tag(val name: String)

@Serializable
data class Info(
	val title: String,
	val description: String,
	val version: String,
)

@Serializable
data class Server(
	val url: String,
	val description: String,
)
