package moe.nea.ledger.utils.telemetry

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class ExceptionContextValue(val exception: Throwable) : ContextValue {
	val stackTrace by lazy {
		exception.stackTraceToString()
	}

	override fun serialize(): JsonElement {
		val jsonObject = JsonObject()
		jsonObject.addProperty("exception_stackTrace", stackTrace)
		jsonObject.add("exception_structure", walkExceptions(exception, 6))
		return jsonObject
	}

	private fun walkExceptions(exception: Throwable, searchDepth: Int): JsonElement {
		val obj = JsonObject()
		obj.addProperty("class", exception.javaClass.name)
		obj.addProperty("message", exception.message)
		// TODO: allow exceptions to implement an "extra info" interface
		if (searchDepth > 0) {
			val cause = exception.cause
			if (cause != null && cause !== exception) {
				obj.add("cause", walkExceptions(cause, searchDepth - 1))
			}
			val suppressions = JsonArray()
			for (suppressedException in exception.suppressedExceptions) {
				suppressions.add(walkExceptions(suppressedException, searchDepth - 1))
			}
			if (suppressions.size() > 0) {
				obj.add("suppressions", suppressions)
			}
		}
		return obj
	}
}
