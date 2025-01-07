package moe.nea.ledger.utils

import io.azam.ulidj.ULID
import java.time.Instant
import kotlin.random.Random

@JvmInline
value class ULIDWrapper(
	val wrapped: String
) {
	companion object {
		fun createULIDAt(timestamp: Instant): ULIDWrapper {
			return ULIDWrapper(ULID.generate(
				timestamp.toEpochMilli(),
				Random.nextBytes(10)
			))
		}
	}

	fun getTimestamp(): Instant {
		return Instant.ofEpochMilli(ULID.getTimestamp(wrapped))
	}

	init {
		require(ULID.isValid(wrapped))
	}
}