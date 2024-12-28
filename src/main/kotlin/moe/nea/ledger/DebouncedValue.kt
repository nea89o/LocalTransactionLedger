package moe.nea.ledger

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class DebouncedValue<T>(private val value: T) {
	companion object {
		fun <T> farFuture(): DebouncedValue<T> {
			val value = DebouncedValue(Unit)
			value.take()
			@Suppress("UNCHECKED_CAST")
			return value as DebouncedValue<T>
		}
	}

	val lastSeenAt = System.nanoTime()
	val age get() = (System.nanoTime() - lastSeenAt).nanoseconds
	var taken = false
		private set

	fun get(debounce: Duration): T? {
		return if (!taken && age >= debounce) value
		else null
	}

	fun replace(): T? {
		return consume(0.seconds)
	}

	fun consume(debounce: Duration): T? {
		return get(debounce)?.also { take() }
	}

	fun take() {
		taken = true
	}
}