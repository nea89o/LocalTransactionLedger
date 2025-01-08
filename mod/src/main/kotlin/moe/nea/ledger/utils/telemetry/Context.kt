package moe.nea.ledger.utils.telemetry

import com.google.gson.JsonObject

class Context(val data: MutableMap<String, ContextValue> = mutableMapOf()) : ContextValue.Collatable<Context> {

	inline fun <reified T : ContextValue> getT(key: String): T? {
		return get(key) as? T
	}

	fun get(key: String): ContextValue? {
		return data[key]
	}

	fun add(key: String, value: ContextValue) {
		data[key] = value
	}

	@Suppress("NOTHING_TO_INLINE")
	private inline fun <T : ContextValue.Collatable<T>> cope(
		left: ContextValue.Collatable<T>,
		right: ContextValue
	): ContextValue {
		return try {
			left.combineWith(right as T)
		} catch (ex: Exception) {
			// TODO: cope with this better
			right
		}
	}

	override fun combineWith(overrides: Context): Context {
		val copy = data.toMutableMap()
		for ((key, overrideValue) in overrides.data) {
			copy.merge(key, overrideValue) { old, new ->
				if (old is ContextValue.Collatable<*>) {
					cope(old, new)
				} else {
					new
				}
			}
		}
		return Context(copy)
	}

	override fun actualize(): Context {
		return this
	}

	override fun serialize(): JsonObject {
		val obj = JsonObject()
		data.forEach { (k, v) ->
			obj.add(k, v.serialize())
		}
		return obj
	}
}