package moe.nea.ledger.database.sql

import java.sql.PreparedStatement

data class LikeClause<T>(val left: Operand<T, String>, val right: StringOperand) : Clause {
	//TODO: check type safety with this one
	override fun asSql(): String {
		return "(" + left.asSql() + " LIKE " + right.asSql() + ")"
	}

	override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
		var index = startIndex
		index = left.appendToStatement(stmt, index)
		index = right.appendToStatement(stmt, index)
		return index
	}
}