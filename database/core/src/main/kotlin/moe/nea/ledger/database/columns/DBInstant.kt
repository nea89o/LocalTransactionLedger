package moe.nea.ledger.database.columns

import moe.nea.ledger.database.DBType
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.Instant

object DBInstant : DBType<Instant, Long> {
	override val dbType: String
		get() = "INTEGER"

	override fun set(stmt: PreparedStatement, index: Int, value: Instant) {
		stmt.setLong(index, value.toEpochMilli())
	}

	override fun get(result: ResultSet, index: Int): Instant {
		return Instant.ofEpochMilli(result.getLong(index))
	}
}