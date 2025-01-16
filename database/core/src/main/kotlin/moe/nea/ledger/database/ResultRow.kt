package moe.nea.ledger.database

import moe.nea.ledger.database.sql.Selectable

class ResultRow(val selectableValues: Map<Selectable<*>, *>) {
	val columnValues = selectableValues.mapNotNull {
		val col = it.key.guessColumn() ?: return@mapNotNull null
		col to it.value
	}.toMap()

	operator fun <T> get(column: Column<T>): T {
		val value = columnValues[column]
			?: error("Invalid column ${column.name}. Only ${columnValues.keys.joinToString { it.name }} are available.")
		return value as T
	}

	operator fun <T> get(column: Selectable<T>): T {
		val value = selectableValues[column]
			?: error("Invalid selectable ${column}. Only ${selectableValues.keys} are available.")
		return value as T
	}
}