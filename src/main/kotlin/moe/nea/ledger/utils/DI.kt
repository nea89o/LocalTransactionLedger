package moe.nea.ledger.utils

import java.lang.reflect.AnnotatedElement
import java.util.Collections
import java.util.Stack

@Suppress("UNCHECKED_CAST")
class DI {
	private fun <T : Any, C> internalProvide(type: Class<T>, element: AnnotatedElement? = null): T {
		val provider = providers[type] as BaseDIProvider<T, C>
		val context = if (element == null) provider.createEmptyContext() else provider.createContext(element)
		val key = Pair(type, context)
		val existingValue = values[key]
		if (existingValue != null) return existingValue as T
		if (type in injectionStack) {
			error("Found injection cycle: ${injectionStack.joinToString(" -> ")} -> $type")
		}
		injectionStack.push(type)
		val value = try {
			provider.provideWithContext(this, context)
		} catch (ex: Exception) {
			throw RuntimeException("Could not create instance for type $type", ex)
		}
		val cycleCheckCookie = injectionStack.pop()
		require(cycleCheckCookie == type) { "Unbalanced stack cookie: $cycleCheckCookie != $type" }
		values[key] = value
		return value
	}

	fun <T : Any> provide(type: Class<T>, element: AnnotatedElement? = null): T {
		return internalProvide<T, Any>(type, element)
	}

	inline fun <reified T : Any> provide(): T = provide(T::class.java)

	fun <T : Any> register(type: Class<T>, provider: BaseDIProvider<T, *>) {
		providers[type] = provider
	}

	fun registerInjectableClasses(vararg type: Class<*>) {
		type.forEach { internalRegisterInjectableClass(it) }
	}

	private fun <T : Any> internalRegisterInjectableClass(type: Class<T>) {
		register(type, DIProvider.fromInjectableClass(type))
	}

	fun instantiateAll() {
		providers.keys.forEach {
			provide(it, null)
		}
	}

	fun getAllInstances(): Collection<Any> =
		Collections.unmodifiableCollection(values.values)

	fun <T : Any> registerSingleton(value: T) {
		register(value.javaClass, DIProvider.singeleton(value))
	}

	private val injectionStack: Stack<Class<*>> = Stack()
	private val values = mutableMapOf<Pair<Class<*>, *>, Any>()
	private val providers = mutableMapOf<Class<*>, BaseDIProvider<*, *>>()

}