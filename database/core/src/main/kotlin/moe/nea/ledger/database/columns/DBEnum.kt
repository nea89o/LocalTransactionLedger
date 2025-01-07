package moe.nea.ledger.database.columns

import moe.nea.ledger.database.DBType
import java.sql.PreparedStatement
import java.sql.ResultSet

class DBEnum<T : Enum<T>>(
	val type: Class<T>,
) : DBType<T> {
	companion object {
		inline operator fun <reified T : Enum<T>> invoke(): DBEnum<T> {
			return DBEnum(T::class.java)
		}
	}

	override val dbType: String
		get() = "TEXT"

	override fun getName(): String {
		return "DBEnum(${type.simpleName})"
	}

	override fun set(stmt: PreparedStatement, index: Int, value: T) {
		stmt.setString(index, value.name)
	}

	override fun get(result: ResultSet, index: Int): T {
		val name = result.getString(index)
		return java.lang.Enum.valueOf(type, name)
	}
}