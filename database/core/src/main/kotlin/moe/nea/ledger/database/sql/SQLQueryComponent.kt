package moe.nea.ledger.database.sql

import java.sql.PreparedStatement

interface SQLQueryComponent {
	fun asSql(): String

	/**
	 * @return the next writable index (should equal to the amount of `?` in [asSql] + [startIndex])
	 */
	fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int

	companion object {
		fun standalone(sql: String): SQLQueryComponent {
			return object : SQLQueryComponent {
				override fun asSql(): String {
					return sql
				}

				override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
					return startIndex
				}
			}
		}
	}
}