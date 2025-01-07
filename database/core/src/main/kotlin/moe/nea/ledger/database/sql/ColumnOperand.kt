package moe.nea.ledger.database.sql

import moe.nea.ledger.database.Column
import java.sql.PreparedStatement

data class ColumnOperand<T>(val column: Column<T>) : Operand<T> {
	override fun asSql(): String {
		return column.qualifiedSqlName
	}

	override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
		return startIndex
	}
}