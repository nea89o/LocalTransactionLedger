import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType


inline fun <reified T : Any> ExtensionAware.configureIf(crossinline block: T.() -> Unit) {
	if (extensions.findByType<T>() != null) {
		extensions.configure<T> { block() }
	}
}

val ktor_version = "3.0.3"

fun DependencyHandlerScope.declareKtorVersion() {
	"implementation"(platform("io.ktor:ktor-bom:$ktor_version"))
}
