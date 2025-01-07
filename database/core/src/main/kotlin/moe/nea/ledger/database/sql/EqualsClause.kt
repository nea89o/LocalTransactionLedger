package moe.nea.ledger.database.sql

import java.sql.PreparedStatement

data class EqualsClause(val left: Operand<*>, val right: Operand<*>) : Clause { // TODO: typecheck this somehow
	override fun asSql(): String {
		return left.asSql() + " = " + right.asSql()
	}

	override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
		var index = startIndex
		index = left.appendToStatement(stmt, index)
		index = right.appendToStatement(stmt, index)
		return index
	}
}