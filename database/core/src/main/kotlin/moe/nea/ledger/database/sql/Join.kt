package moe.nea.ledger.database.sql

import moe.nea.ledger.database.Table
import java.sql.PreparedStatement

data class Join(
	val table: Table,
//TODO: aliased columns	val tableAlias: String,
	val filter: Clause,
) : SQLQueryComponent {
	//	JOIN ItemEntry on LogEntry.transactionId = ItemEntry.transactionId
	override fun asSql(): String {
		return "JOIN ${table.sqlName} ON ${filter.asSql()}"
	}

	override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
		return filter.appendToStatement(stmt, startIndex)
	}
}