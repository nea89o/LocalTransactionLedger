package moe.nea.ledger.database

import moe.nea.ledger.database.sql.IntoSelectable
import moe.nea.ledger.database.sql.Selectable
import java.sql.PreparedStatement

class Column<T> @Deprecated("Use Table.column instead") constructor(
	val table: Table,
	val name: String,
	val type: DBType<T>
) : IntoSelectable<T> {
	override fun asSelectable() = object : Selectable<T> {
		override fun asSql(): String {
			return qualifiedSqlName
		}

		override val dbType: DBType<T>
			get() = this@Column.type

		override fun guessColumn(): Column<T>? {
			return this@Column
		}

		override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
			return startIndex
		}
	}

	val sqlName get() = "`$name`"
	val qualifiedSqlName get() = table.sqlName + "." + sqlName
}