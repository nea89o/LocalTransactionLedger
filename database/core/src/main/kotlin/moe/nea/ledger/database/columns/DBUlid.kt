package moe.nea.ledger.database.columns

import moe.nea.ledger.utils.ULIDWrapper
import moe.nea.ledger.database.DBType
import java.sql.PreparedStatement
import java.sql.ResultSet

object DBUlid : DBType<ULIDWrapper> {
	override val dbType: String
		get() = "TEXT"

	override fun get(result: ResultSet, index: Int): ULIDWrapper {
		val text = result.getString(index)
		return ULIDWrapper(text)
	}

	override fun set(stmt: PreparedStatement, index: Int, value: ULIDWrapper) {
		stmt.setString(index, value.wrapped)
	}
}

