package moe.nea.ledger.utils.network

import com.google.gson.JsonElement
import java.net.URL

data class Request(
	val url: URL,
	val method: Method,
	val body: String?,
	val headers: Map<String, String>,
) {
	enum class Method {
		GET, POST
	}

	enum class MediaType(val text: String) {
		JSON("application/json"),
		TEXT("text/plain"),
		HTML("text/html"),
		ANY("*/*"),
	}

	fun withHeaders(map: Map<String, String>): Request {
		// TODO: enforce caselessness?
		return this.copy(headers = headers + map)
	}

	fun post() = copy(method = Method.POST)
	fun get() = copy(method = Method.GET)

	fun json(element: JsonElement) = copy(
		headers = headers + mapOf("content-type" to "application/json"),
		body = element.toString())

	fun accept(request: MediaType) = withHeaders(mapOf("accept" to request.text))

	fun acceptJson() = accept(MediaType.JSON)

	fun execute(requestUtil: RequestUtil) = requestUtil.executeRequest(this)
}