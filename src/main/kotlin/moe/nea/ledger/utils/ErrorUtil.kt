package moe.nea.ledger.utils

import moe.nea.ledger.utils.di.Inject
import moe.nea.ledger.utils.telemetry.ContextValue
import moe.nea.ledger.utils.telemetry.EventRecorder
import moe.nea.ledger.utils.telemetry.Span
import java.util.concurrent.CompletableFuture

class ErrorUtil {

	@Inject
	lateinit var reporter: EventRecorder

	fun reportAdHoc(message: String) {
		report(Exception(message), message)

	}

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

	fun <T : CompletableFuture<*>> listenToFuture(t: T): T {
		t.handle { ignored, exception ->
			if (exception != null)
				report(exception, "Uncaught exception in completable future")
		}
		return t
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