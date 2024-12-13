package moe.nea.ledger.utils

import moe.nea.ledger.utils.telemetry.ContextValue
import moe.nea.ledger.utils.telemetry.EventRecorder
import moe.nea.ledger.utils.telemetry.Span

class ErrorUtil {

	@Inject
	lateinit var reporter: EventRecorder

	fun report(exception: Throwable, message: String?) {
		Span.current().recordException(reporter, exception, message)
	}

	fun <T> Result<T>.getOrReport(): T? {
		val exc = exceptionOrNull()
		if (exc != null) {
			report(exc, null)
		}
		return getOrNull()
	}

	inline fun <T> catch(
		vararg pairs: Pair<String, ContextValue>,
		crossinline function: () -> T
	): T? {
		return Span.current().enterWith(*pairs) {
			try {
				return@enterWith function()
			} catch (ex: Exception) {
				report(ex, null)
				return@enterWith null
			}
		}
	}
}