package moe.nea.ledger

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NumberUtilKtTest {
	@Test
	fun parseRomanNumberTest() {
		assertEquals(4, parseRomanNumber("IV"))
		assertEquals(1, parseRomanNumber("I"))
		assertEquals(14, parseRomanNumber("XIV"))
		assertEquals(3, parseRomanNumber("III"))
		assertEquals(8, parseRomanNumber("IIX"))
		assertEquals(500, parseRomanNumber("DM"))
		assertEquals(2024, parseRomanNumber("MMXXIV"))
	}
}