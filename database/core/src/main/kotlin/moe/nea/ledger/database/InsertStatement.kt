package moe.nea.ledger.database

class InsertStatement(val properties: MutableMap<Column<*, *>, Any>) {
	operator fun <T : Any> set(key: Column<T, *>, value: T) {
		properties[key] = value
	}
}