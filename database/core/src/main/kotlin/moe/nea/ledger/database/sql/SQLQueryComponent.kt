package moe.nea.ledger.database.sql

import java.sql.PreparedStatement

interface SQLQueryComponent {
	fun asSql(): String

	/**
	 * @return the next writable index (should equal to the amount of `?` in [asSql] + [startIndex])
	 */
	fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int

	companion object {
		fun composite(vararg elements: SQLQueryComponent): SQLQueryComponent {
			return object : SQLQueryComponent {
				override fun asSql(): String {
					return elements.joinToString(" ") { it.asSql() }
				}

				override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
					var index = startIndex
					for (element in elements) {
						val lastIndex = index
						index = element.appendToStatement(stmt, index)
						require(lastIndex <= index) { "$element just tried to go back in time $index < $lastIndex" }
					}
					return index

				}
			}
		}

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