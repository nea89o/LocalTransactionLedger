package moe.nea.ledger.utils.network

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import moe.nea.ledger.utils.telemetry.ContextValue

class RequestTrace(val request: Request) : ContextValue {
	override fun serialize(): JsonElement {
		return JsonObject().apply {
			addProperty("url", request.url.toString())
			addProperty("method", request.method.name)
			addProperty("content-type", request.headers["content-type"])
			addProperty("accept", request.headers["accept"])
		}
	}

	companion object {
		val KEY = "http_request"
		fun createTrace(request: Request): Pair<String, RequestTrace> = KEY to RequestTrace(request)
	}
}