import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType


inline fun <reified T : Any> ExtensionAware.configureIf(crossinline block: T.() -> Unit) {
	if (extensions.findByType<T>() != null) {
		extensions.configure<T> { block() }
	}
}
