package moe.nea.ledger.utils.di

import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Constructor

fun interface DIProvider<T : Any> : BaseDIProvider<T, Unit> {
	override fun provideWithContext(di: DI, context: Unit): T {
		return provide(di)
	}

	override fun createContext(element: AnnotatedElement) {
	}

	override fun createEmptyContext() {
	}

	fun provide(di: DI): T

	companion object {

		fun <T : Any> fromInjectableClass(clazz: Class<T>): DIProvider<T> {
			@Suppress("UNCHECKED_CAST")
			val cons = (clazz.constructors.find { it.getAnnotation(Inject::class.java) != null }
				?: clazz.constructors.find { it.parameterCount == 0 }
				?: error("Could not find DI injection entrypoint for class $clazz"))
					as Constructor<out T>
			// TODO: consider using unsafe init to inject the parameters *before* calling the constructor
			return DIProvider { di ->
				val typArgs = cons.parameters.map {
					di.provide(it.type, it)
				}.toTypedArray()
				val instance = cons.newInstance(*typArgs)
				for (it in clazz.fields) {
					if (it.getAnnotation(Inject::class.java) == null) continue
					it.set(instance, di.provide(it.type, it))
				}
				instance
			}
		}

		fun <T : Any> singeleton(value: T): DIProvider<T> {
			return DIProvider { _ -> value }
		}
	}

}

interface BaseDIProvider<T : Any, C> {
	fun createContext(element: AnnotatedElement): C
	fun provideWithContext(di: DI, context: C): T
	fun createEmptyContext(): C
}
