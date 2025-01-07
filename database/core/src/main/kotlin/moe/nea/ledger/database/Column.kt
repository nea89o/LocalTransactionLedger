package moe.nea.ledger.database

class Column<T> @Deprecated("Use Table.column instead") constructor(
	val table: Table,
	val name: String,
	val type: DBType<T>
) {
	val sqlName get() = "`$name`"
	val qualifiedSqlName get() = table.sqlName + "." + sqlName
}