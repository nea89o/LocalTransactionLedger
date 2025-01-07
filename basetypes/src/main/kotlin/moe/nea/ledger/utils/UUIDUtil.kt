package moe.nea.ledger.utils

import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

object UUIDUtil {
	@OptIn(ExperimentalUuidApi::class)
	fun parsePotentiallyDashlessUUID(str: String): UUID {
		val bytes = ByteArray(16)
		var i = -1
		var bi = 0
		while (++i < str.length) {
			val char = str[i]
			if (char == '-') {
				if (bi != 4 && bi != 6 && bi != 8 && bi != 10) {
					error("Unexpected dash in UUID: $str")
				}
				continue
			}
			val current = parseHexDigit(str, char)
			++i
			if (i >= str.length)
				error("Unexpectedly short UUID: $str")
			val next = parseHexDigit(str, str[i])
			bytes[bi++] = (current * 16 or next).toByte()
		}
		if (bi != 16)
			error("Unexpectedly short UUID: $str")
		return Uuid.fromByteArray(bytes).toJavaUuid()
	}

	private fun parseHexDigit(str: String, char: Char): Int {
		val d = char - '0'
		if (d < 10) return d
		val h = char - 'a'
		if (h < 6) return 10 + h
		error("Unexpected hex digit $char in UUID: $str")
	}
}