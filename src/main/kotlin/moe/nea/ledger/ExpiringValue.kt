package moe.nea.ledger

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

class ExpiringValue<T>(private val value: T) {
	val lastSeenAt: Long = System.nanoTime()
	val age get() = (System.nanoTime() - lastSeenAt).nanoseconds
	var taken = false
		private set

	fun get(expiry: Duration): T? {
		return if (!taken && age < expiry) value
		else null
	}

	companion object {
		fun <T> empty(): ExpiringValue<T> {
			val value = ExpiringValue(Unit)
			value.take()
			@Suppress("UNCHECKED_CAST")
			return value as ExpiringValue<T>
		}
	}

	fun consume(expiry: Duration): T? = get(expiry)?.also { take() }
	fun take() {
		taken = true
	}
}