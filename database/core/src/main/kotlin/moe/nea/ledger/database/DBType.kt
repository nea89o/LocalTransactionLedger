package moe.nea.ledger.database

import java.sql.PreparedStatement
import java.sql.ResultSet

interface DBType<T> {
	val dbType: String

	fun get(result: ResultSet, index: Int): T
	fun set(stmt: PreparedStatement, index: Int, value: T)
	fun getName(): String = javaClass.simpleName
	fun <R> mapped(
		from: (R) -> T,
		to: (T) -> R,
	): DBType<R> {
		return object : DBType<R> {
			override fun getName(): String {
				return "Mapped(${this@DBType.getName()})"
			}

			override val dbType: String
				get() = this@DBType.dbType

			override fun get(result: ResultSet, index: Int): R {
				return to(this@DBType.get(result, index))
			}

			override fun set(stmt: PreparedStatement, index: Int, value: R) {
				this@DBType.set(stmt, index, from(value))
			}
		}
	}
}