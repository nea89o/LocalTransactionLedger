package moe.nea.ledger.database.columns

import moe.nea.ledger.database.DBType
import java.sql.PreparedStatement
import java.sql.ResultSet

object DBDouble : DBType<Double, Double> {
	override val dbType: String
		get() = "DOUBLE"

	override fun get(result: ResultSet, index: Int): Double {
		return result.getDouble(index)
	}

	override fun set(stmt: PreparedStatement, index: Int, value: Double) {
		stmt.setDouble(index, value)
	}
}