package moe.nea.ledger.utils.telemetry

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive

class BooleanContext(val boolean: Boolean) : ContextValue {
	override fun serialize(): JsonElement {
		return JsonPrimitive(boolean)
	}
}
