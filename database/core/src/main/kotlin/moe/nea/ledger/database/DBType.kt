package moe.nea.ledger.database

import moe.nea.ledger.database.sql.ClauseBuilder
import java.sql.PreparedStatement
import java.sql.ResultSet


interface DBType<
		/**
		 * Mapped type of this db type. Represents the Java type this db type accepts for saving to the database.
		 */
		T,
		/**
		 * Phantom marker type representing how this db type is presented to the actual DB. Is used by APIs such as [ClauseBuilder] to allow for rough typechecking.
		 */
		RawType> {
	val dbType: String

	fun get(result: ResultSet, index: Int): T
	fun set(stmt: PreparedStatement, index: Int, value: T)
	fun getName(): String = javaClass.simpleName
	fun <R> mapped(
		from: (R) -> T,
		to: (T) -> R,
	): DBType<R, RawType> {
		return object : DBType<R, RawType> {
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