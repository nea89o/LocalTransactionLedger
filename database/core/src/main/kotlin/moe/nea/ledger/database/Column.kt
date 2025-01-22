package moe.nea.ledger.database

import moe.nea.ledger.database.sql.IntoSelectable
import moe.nea.ledger.database.sql.Selectable
import java.sql.PreparedStatement

class Column<T, Raw> @Deprecated("Use Table.column instead") constructor(
	val table: Table,
	val name: String,
	val type: DBType<T, Raw>
) : IntoSelectable<T> {
	override fun asSelectable() = object : Selectable<T, Raw> {
		override fun asSql(): String {
			return qualifiedSqlName
		}

		override val dbType: DBType<T, Raw>
			get() = this@Column.type

		override fun guessColumn(): Column<T, Raw> {
			return this@Column
		}

		override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
			return startIndex
		}
	}

	val sqlName get() = "`$name`"
	val qualifiedSqlName get() = table.sqlName + "." + sqlName
}