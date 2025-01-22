package moe.nea.ledger.database.sql

import java.sql.PreparedStatement

data class StringOperand(val value: String) : Operand<String, String> {
	override fun asSql(): String {
		return "?"
	}

	override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
		stmt.setString(startIndex, value)
		return 1 + startIndex
	}
}