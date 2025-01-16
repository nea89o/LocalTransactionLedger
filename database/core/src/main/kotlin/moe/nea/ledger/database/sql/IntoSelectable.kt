package moe.nea.ledger.database.sql

interface IntoSelectable<T> {
	fun asSelectable(): Selectable<T>
}