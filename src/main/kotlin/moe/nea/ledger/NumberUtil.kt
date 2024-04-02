package moe.nea.ledger

import java.util.regex.Matcher
import java.util.regex.Pattern

// language=regexp
val SHORT_NUMBER_PATTERN = "[0-9]+(?:,[0-9]+)*(?:\\.[0-9]+)?[kKmMbB]?"

val siScalars = mapOf(
    'k' to 1_000.0,
    'K' to 1_000.0,
    'm' to 1_000_000.0,
    'M' to 1_000_000.0,
    'b' to 1_000_000_000.0,
    'B' to 1_000_000_000.0,
)

fun parseShortNumber(string: String): Double {
    var k = string.replace(",", "")
    val scalar = k.last()
    var scalarMultiplier = siScalars[scalar]
    if (scalarMultiplier == null) {
        scalarMultiplier = 1.0
    } else {
        k = k.dropLast(1)
    }
    return k.toDouble() * scalarMultiplier
}

inline fun <T> Pattern.useMatcher(string: String, block: Matcher.() -> T): T? =
    matcher(string).takeIf { it.matches() }?.let(block)

fun String.unformattedString(): String = replace("§.".toRegex(), "")