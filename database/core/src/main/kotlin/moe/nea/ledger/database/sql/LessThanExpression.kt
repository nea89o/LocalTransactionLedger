package moe.nea.ledger.database.sql

import java.sql.PreparedStatement

class LessThanExpression(val lhs: Operand<*>, val rhs: Operand<*>) :
	BooleanExpression {
	override fun asSql(): String {
		return "${lhs.asSql()} < ${rhs.asSql()}"
	}

	override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
		val next = lhs.appendToStatement(stmt, startIndex)
		return rhs.appendToStatement(stmt, next)
	}
}
