package moe.nea.ledger.database

import java.sql.Connection

abstract class Table(val name: String) {
	val sqlName get() = "`$name`"
	protected val _mutable_columns: MutableList<Column<*, *>> = mutableListOf()
	protected val _mutable_constraints: MutableList<Constraint> = mutableListOf()
	val columns: List<Column<*, *>> get() = _mutable_columns
	val constraints get() = _mutable_constraints
	protected fun unique(vararg columns: Column<*, *>) {
		_mutable_constraints.add(UniqueConstraint(columns.toList()))
	}

	protected fun <T, R> column(name: String, type: DBType<T, R>): Column<T, R> {
		@Suppress("DEPRECATION") val column = Column(this, name, type)
		_mutable_columns.add(column)
		return column
	}

	fun debugSchema() {
		val nameWidth = columns.maxOf { it.name.length }
		val typeWidth = columns.maxOf { it.type.getName().length }
		val totalWidth = maxOf(2 + nameWidth + 3 + typeWidth + 2, name.length + 4)
		val adjustedTypeWidth = totalWidth - nameWidth - 2 - 3 - 2

		var string = "\n"
		string += ("+" + "-".repeat(totalWidth - 2) + "+\n")
		string += ("| $name${" ".repeat(totalWidth - 4 - name.length)} |\n")
		string += ("+" + "-".repeat(totalWidth - 2) + "+\n")
		for (column in columns) {
			string += ("| ${column.name}${" ".repeat(nameWidth - column.name.length)} |")
			string += (" ${column.type.getName()}" +
					"${" ".repeat(adjustedTypeWidth - column.type.getName().length)} |\n")
		}
		string += ("+" + "-".repeat(totalWidth - 2) + "+")
		println(string)
	}

	fun createIfNotExists(
		connection: Connection,
		filteredColumns: List<Column<*, *>> = columns
	) {
		val properties = mutableListOf<String>()
		for (column in filteredColumns) {
			properties.add("${column.sqlName} ${column.type.dbType}")
		}
		val columnSet = filteredColumns.toSet()
		for (constraint in constraints) {
			if (columnSet.containsAll(constraint.affectedColumns)) {
				properties.add(constraint.asSQL())
			}
		}
		connection.prepareAndLog("CREATE TABLE IF NOT EXISTS $sqlName (" + properties.joinToString() + ")")
			.execute()
	}

	fun alterTableAddColumns(
		connection: Connection,
		newColumns: List<Column<*, *>>
	) {
		for (column in newColumns) {
			connection.prepareAndLog("ALTER TABLE $sqlName ADD ${column.sqlName} ${column.type.dbType}")
				.execute()
		}
		for (constraint in constraints) {
			// TODO: automatically add constraints, maybe (or maybe move constraints into the upgrade schema)
		}
	}

	enum class OnConflict {
		FAIL,
		IGNORE,
		REPLACE,
		;

		fun asSql(): String {
			return name
		}
	}

	fun insert(connection: Connection, onConflict: OnConflict = OnConflict.FAIL, block: (InsertStatement) -> Unit) {
		val insert = InsertStatement(HashMap())
		block(insert)
		require(insert.properties.keys == columns.toSet())
		val columnNames = columns.joinToString { it.sqlName }
		val valueNames = columns.joinToString { "?" }
		val statement =
			connection.prepareAndLog("INSERT OR ${onConflict.asSql()} INTO $sqlName ($columnNames) VALUES ($valueNames)")
		for ((index, column) in columns.withIndex()) {
			(column as Column<Any, *>).type.set(statement, index + 1, insert.properties[column]!!)
		}
		statement.execute()
	}

	fun from(connection: Connection): Query {
		return Query(connection, mutableListOf(), this)
	}

	fun selectAll(connection: Connection): Query {
		return Query(connection, columns.mapTo(mutableListOf()) { it.asSelectable() }, this)
	}
}