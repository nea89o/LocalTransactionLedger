package moe.nea.ledger.database

interface Constraint {
	val affectedColumns: Collection<Column<*>>
	fun asSQL(): String
}