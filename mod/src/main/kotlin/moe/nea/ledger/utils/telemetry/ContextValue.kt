package moe.nea.ledger.utils.telemetry

import com.google.gson.JsonElement
import com.google.gson.JsonObject

interface ContextValue {
	companion object {
		fun <T : Collatable<T>> lazyCollatable(value: () -> Collatable<T>): Collatable<T> {
			return LazyCollatable(value)
		}

		fun lazy(value: () -> ContextValue): ContextValue {
			return object : ContextValue {
				val value by kotlin.lazy(value)
				override fun serialize(): JsonElement {
					return this.value.serialize()
				}
			}
		}

		fun bool(boolean: Boolean): ContextValue {
			return BooleanContext(boolean)
		}

		fun string(message: String): ContextValue {
			return StringContext(message)
		}

		fun jsonObject(vararg pairs: Pair<String, JsonElement>): ContextValue {
			val obj = JsonObject()
			for ((l, r) in pairs) {
				obj.add(l, r)
			}
			return JsonElementContext(obj)
		}

		fun compound(vararg pairs: Pair<String, String>): ContextValue {
			val obj = JsonObject()
			for ((l, r) in pairs) {
				obj.addProperty(l, r)
			}
			// TODO: should this be its own class?
			return JsonElementContext(obj)
		}
	}

	// TODO: allow other serialization formats
	fun serialize(): JsonElement
	interface Collatable<T : Collatable<T>> : ContextValue {
		fun combineWith(overrides: T): T
		fun actualize(): T
	}

	private class LazyCollatable<T : Collatable<T>>(
		provider: () -> Collatable<T>,
	) : Collatable<T> {
		val value by kotlin.lazy(provider)
		override fun actualize(): T {
			return value.actualize()
		}

		override fun combineWith(overrides: T): T {
			return value.combineWith(overrides)
		}

		override fun serialize(): JsonElement {
			return value.serialize()
		}
	}
}
