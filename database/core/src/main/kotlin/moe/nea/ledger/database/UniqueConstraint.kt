package moe.nea.ledger.database

class UniqueConstraint(val columns: List<Column<*>>) : Constraint {
	init {
		require(columns.isNotEmpty())
	}

	override val affectedColumns: Collection<Column<*>>
		get() = columns

	override fun asSQL(): String {
		return "UNIQUE (${columns.joinToString() { it.sqlName }})"
	}
}