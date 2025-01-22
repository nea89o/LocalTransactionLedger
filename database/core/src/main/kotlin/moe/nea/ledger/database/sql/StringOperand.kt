package moe.nea.ledger.database.sql

import java.sql.PreparedStatement

/**
 * As opposed to just any [Operand<*, String>][Operand], this string operand represents a string operand that is part of the query, as opposed to potentially the state of a column.
 */
data class StringOperand(val value: String) : Operand<String, String> {
	override fun asSql(): String {
		return "?"
	}

	override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
		stmt.setString(startIndex, value)
		return 1 + startIndex
	}
}