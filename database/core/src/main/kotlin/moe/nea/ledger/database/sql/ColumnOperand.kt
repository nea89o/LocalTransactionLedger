package moe.nea.ledger.database.sql

import moe.nea.ledger.database.Column
import moe.nea.ledger.database.DBType
import java.sql.PreparedStatement

data class ColumnOperand<T, Raw>(val column: Column<T, Raw>) : TypedOperand<T, Raw>() {
	override val dbType: DBType<T, Raw>
		get() = column.type

	override fun asSql(): String {
		return column.qualifiedSqlName
	}

	override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
		return startIndex
	}
}