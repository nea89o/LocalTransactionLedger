package moe.nea.ledger.database.sql

import java.sql.PreparedStatement

data class ANDExpression(
	val elements: List<BooleanExpression>
) : BooleanExpression {
	init {
		require(elements.isNotEmpty())
	}

	override fun asSql(): String {
		elements.singleOrNull()?.let {
			return "(" + it.asSql() + ")"
		}
		return elements.joinToString(" AND ", "(", ")") { it.asSql() }
	}

	override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
		var index = startIndex
		for (element in elements) {
			index = element.appendToStatement(stmt, index)
		}
		return index
	}
}