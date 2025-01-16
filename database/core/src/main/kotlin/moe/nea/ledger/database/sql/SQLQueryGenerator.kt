package moe.nea.ledger.database.sql

import moe.nea.ledger.database.prepareAndLog
import java.sql.Connection
import java.sql.PreparedStatement

object SQLQueryGenerator {
	fun List<SQLQueryComponent>.concatToFilledPreparedStatement(connection: Connection): PreparedStatement {
		val query = StringBuilder()
		for (element in this) {
			if (query.isNotEmpty()) {
				query.append(" ")
			}
			query.append(element.asSql())
		}
		val statement = connection.prepareAndLog(query.toString())
		var index = 1
		for (element in this) {
			val nextIndex = element.appendToStatement(statement, index)
			if (nextIndex < index) error("$element went back in time")
			index = nextIndex
		}
		return statement
	}
}
