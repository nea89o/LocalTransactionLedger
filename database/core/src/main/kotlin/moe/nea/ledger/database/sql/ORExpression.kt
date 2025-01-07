package moe.nea.ledger.database.sql

import java.sql.PreparedStatement

data class ORExpression(
	val elements: List<BooleanExpression>
) : BooleanExpression {
	init {
		require(elements.isNotEmpty())
	}

	override fun asSql(): String {
		return (elements + SQLQueryComponent.standalone("FALSE")).joinToString(" OR ", "(", ")") { it.asSql() }
	}

	override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
		var index = startIndex
		for (element in elements) {
			index = element.appendToStatement(stmt, index)
		}
		return index
	}
}