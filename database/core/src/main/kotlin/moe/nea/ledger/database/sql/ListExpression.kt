package moe.nea.ledger.database.sql

import moe.nea.ledger.database.DBType
import java.sql.PreparedStatement

data class ListExpression<T, R>(
	val elements: List<T>,
	val dbType: DBType<T, R>
) : Operand<List<T>, List<R>> {
	override fun asSql(): String {
		return elements.joinToString(prefix = "(", postfix = ")") { "?" }
	}

	override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
		var index = startIndex
		for (element in elements) {
			dbType.set(stmt, index, element)
			index++
		}
		return index
	}
}
