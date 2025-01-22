package moe.nea.ledger.database.sql

import moe.nea.ledger.database.Column
import java.sql.PreparedStatement

data class ColumnOperand<T, Raw>(val column: Column<T, Raw>) : Operand<T, Raw> {
	override fun asSql(): String {
		return column.qualifiedSqlName
	}

	override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
		return startIndex
	}
}