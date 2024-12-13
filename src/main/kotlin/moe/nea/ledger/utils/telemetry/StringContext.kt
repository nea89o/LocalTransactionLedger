package moe.nea.ledger.utils.telemetry

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

class StringContext(val message: String) : ContextValue {
	override fun serialize(): JsonElement {
		return JsonPrimitive(message)
	}

}
