package moe.nea.ledger.utils.di

import java.lang.reflect.AnnotatedElement
import java.util.Collections
import java.util.Stack

@Suppress("UNCHECKED_CAST")
class DI {
	private fun formatInjectionStack() =
		injectionStack.joinToString(" -> ")

	fun <T : Any> getProvider(type: Class<T>): BaseDIProvider<T, *> {
		val provider = providers[type] as BaseDIProvider<T, *>?
			?: error("Could not find provider for type $type")
		return provider
	}

	private fun <T : Any, C> internalProvide(type: Class<T>, element: AnnotatedElement? = null): T {
		try {
			val provider = getProvider(type) as BaseDIProvider<T, C>
			val context = if (element == null) provider.createEmptyContext() else provider.createContext(element)
			val key = Pair(type, context)
			val existingValue = values[key]
			if (existingValue != null) return existingValue as T
			if (type in injectionStack) {
				error("Found injection cycle: ${formatInjectionStack()} -> $type")
			}
			injectionStack.push(type)
			val value =
				provider.provideWithContext(this, context)
			val cycleCheckCookie = injectionStack.pop()
			require(cycleCheckCookie == type) { "Unbalanced stack cookie: $cycleCheckCookie != $type" }
			values[key] = value
			return value
		} catch (ex: Exception) {
			throw RuntimeException("Could not create instance for type $type (in stack ${formatInjectionStack()})", ex)
		}
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

	init {
		registerSingleton(this)
	}
}