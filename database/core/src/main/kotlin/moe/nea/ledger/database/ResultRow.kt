package moe.nea.ledger.database

class ResultRow(val columnValues: Map<Column<*>, *>) {
	operator fun <T> get(column: Column<T>): T {
		val value = columnValues[column]
			?: error("Invalid column ${column.name}. Only ${columnValues.keys.joinToString { it.name }} are available.")
		return value as T
	}
}