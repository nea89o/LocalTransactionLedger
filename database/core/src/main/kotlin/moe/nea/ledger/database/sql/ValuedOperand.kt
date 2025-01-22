package moe.nea.ledger.database.sql

import moe.nea.ledger.database.DBType
import java.sql.PreparedStatement

class ValuedOperand<T, R>(val dbType: DBType<T, R>, val value: T) : Operand<T, R> {
	override fun asSql(): String {
		return "?"
	}

	override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
		dbType.set(stmt, startIndex, value)
		return startIndex + 1
	}
}
