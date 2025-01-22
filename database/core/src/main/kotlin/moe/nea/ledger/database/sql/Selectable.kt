package moe.nea.ledger.database.sql

import moe.nea.ledger.database.Column
import moe.nea.ledger.database.DBType

/**
 * Something that can be selected. Like a column, or an expression thereof
 */
interface Selectable<T, Raw> : SQLQueryComponent, IntoSelectable<T> {
	override fun asSelectable(): Selectable<T, Raw> {
		return this
	}

	val dbType: DBType<T, Raw>
	fun guessColumn(): Column<T, *>?
}

