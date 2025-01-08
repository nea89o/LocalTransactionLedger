package moe.nea.ledger.utils.telemetry

import com.google.gson.JsonElement

class JsonElementContext(val element: JsonElement) : ContextValue {
	override fun serialize(): JsonElement {
		return element
	}
}
