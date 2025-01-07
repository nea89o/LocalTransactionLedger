package moe.nea.ledger.database.columns

import moe.nea.ledger.database.DBType
import java.sql.PreparedStatement
import java.sql.ResultSet

object DBInt : DBType<Long> {
	override val dbType: String
		get() = "INTEGER"

	override fun get(result: ResultSet, index: Int): Long {
		return result.getLong(index)
	}

	override fun set(stmt: PreparedStatement, index: Int, value: Long) {
		stmt.setLong(index, value)
	}
}