package moe.nea.ledger.database.columns

import moe.nea.ledger.database.DBType
import moe.nea.ledger.utils.UUIDUtil
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.UUID

object DBUuid : DBType<UUID, String> {
	override val dbType: String
		get() = "TEXT"

	override fun get(result: ResultSet, index: Int): UUID {
		return UUIDUtil.parsePotentiallyDashlessUUID(result.getString(index))
	}

	override fun set(stmt: PreparedStatement, index: Int, value: UUID) {
		stmt.setString(index, value.toString())
	}
}