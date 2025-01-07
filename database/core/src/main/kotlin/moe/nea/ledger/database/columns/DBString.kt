package moe.nea.ledger.database.columns

import moe.nea.ledger.database.DBType
import java.sql.PreparedStatement
import java.sql.ResultSet

object DBString : DBType<String> {
	override val dbType: String
		get() = "TEXT"

	override fun get(result: ResultSet, index: Int): String {
		return result.getString(index)
	}

	override fun set(stmt: PreparedStatement, index: Int, value: String) {
		stmt.setString(index, value)
	}
}