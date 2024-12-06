package moe.nea.ledger.database

import moe.nea.ledger.UUIDUtil
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

interface DBSchema {
	val tables: List<Table>
}

interface DBType<T> {
	val dbType: String

	fun get(result: ResultSet, index: Int): T
	fun set(stmt: PreparedStatement, index: Int, value: T)
	fun getName(): String = javaClass.simpleName
	fun <R> mapped(
		from: (R) -> T,
		to: (T) -> R,
	): DBType<R> {
		return object : DBType<R> {
			override fun getName(): String {
				return "Mapped(${this@DBType.getName()})"
			}

			override val dbType: String
				get() = this@DBType.dbType

			override fun get(result: ResultSet, index: Int): R {
				return to(this@DBType.get(result, index))
			}

			override fun set(stmt: PreparedStatement, index: Int, value: R) {
				this@DBType.set(stmt, index, from(value))
			}
		}
	}
}

object DBUuid : DBType<UUID> {
	override val dbType: String
		get() = "TEXT"

	override fun get(result: ResultSet, index: Int): UUID {
		return UUIDUtil.parseDashlessUuid(result.getString(index))
	}

	override fun set(stmt: PreparedStatement, index: Int, value: UUID) {
		stmt.setString(index, value.toString())
	}
}

object DBUlid : DBType<UUIDUtil.ULIDWrapper> {
	override val dbType: String
		get() = "TEXT"

	override fun get(result: ResultSet, index: Int): UUIDUtil.ULIDWrapper {
		val text = result.getString(index)
		return UUIDUtil.ULIDWrapper(text)
	}

	override fun set(stmt: PreparedStatement, index: Int, value: UUIDUtil.ULIDWrapper) {
		stmt.setString(index, value.wrapped)
	}
}

object DBString : DBType<String> {
	override val dbType: String
		get() = "TEXT"

	override fun get(result: ResultSet, index: Int): String {
		return result.getString(index)
	}

	override fun set(stmt: PreparedStatement, index: Int, value: String) {
		stmt.setString(index, value)
	}
}

class DBEnum<T : Enum<T>>(
	val type: Class<T>,
) : DBType<T> {
	companion object {
		inline operator fun <reified T : Enum<T>> invoke(): DBEnum<T> {
			return DBEnum(T::class.java)
		}
	}

	override val dbType: String
		get() = "TEXT"

	override fun getName(): String {
		return "DBEnum(${type.simpleName})"
	}

	override fun set(stmt: PreparedStatement, index: Int, value: T) {
		stmt.setString(index, value.name)
	}

	override fun get(result: ResultSet, index: Int): T {
		val name = result.getString(index)
		return java.lang.Enum.valueOf(type, name)
	}
}

object DBDouble : DBType<Double> {
	override val dbType: String
		get() = "DOUBLE"

	override fun get(result: ResultSet, index: Int): Double {
		return result.getDouble(index)
	}

	override fun set(stmt: PreparedStatement, index: Int, value: Double) {
		stmt.setDouble(index, value)
	}
}

object DBInt : DBType<Long> {
	override val dbType: String
		get() = "INTEGER"

	override fun get(result: ResultSet, index: Int): Long {
		return result.getLong(index)
	}

	override fun set(stmt: PreparedStatement, index: Int, value: Long) {
		stmt.setLong(index, value)
	}
}

object DBInstant : DBType<Instant> {
	override val dbType: String
		get() = "INTEGER"

	override fun set(stmt: PreparedStatement, index: Int, value: Instant) {
		stmt.setLong(index, value.toEpochMilli())
	}

	override fun get(result: ResultSet, index: Int): Instant {
		return Instant.ofEpochMilli(result.getLong(index))
	}
}

class Column<T> @Deprecated("Use Table.column instead") constructor(val name: String, val type: DBType<T>) {
	val sqlName get() = "`$name`"
}

interface Constraint {
	val affectedColumns: Collection<Column<*>>
	fun asSQL(): String
}

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

abstract class Table(val name: String) {
	val sqlName get() = "`$name`"
	protected val _mutable_columns: MutableList<Column<*>> = mutableListOf()
	protected val _mutable_constraints: MutableList<Constraint> = mutableListOf()
	val columns: List<Column<*>> get() = _mutable_columns
	val constraints get() = _mutable_constraints
	protected fun unique(vararg columns: Column<*>) {
		_mutable_constraints.add(UniqueConstraint(columns.toList()))
	}

	protected fun <T> column(name: String, type: DBType<T>): Column<T> {
		@Suppress("DEPRECATION") val column = Column(name, type)
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
		filteredColumns: List<Column<*>> = columns
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
		newColumns: List<Column<*>>
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
			(column as Column<Any>).type.set(statement, index + 1, insert.properties[column]!!)
		}
		statement.execute()
	}


	fun selectAll(connection: Connection): Query {
		return Query(connection, columns, this)
	}
}

class InsertStatement(val properties: MutableMap<Column<*>, Any>) {
	operator fun <T : Any> set(key: Column<T>, value: T) {
		properties[key] = value
	}
}

fun Connection.prepareAndLog(statement: String): PreparedStatement {
	println("Preparing to execute $statement")
	return prepareStatement(statement)
}

class Query(
	val connection: Connection,
	val selectedColumns: List<Column<*>>,
	var table: Table,
	var limit: UInt? = null,
	var skip: UInt? = null,
//	var order: OrderClause?= null,
//	val condition: List<SqlCondition>
) : Iterable<ResultRow> {
	fun skip(skip: UInt): Query {
		require(limit != null)
		this.skip = skip
		return this
	}

	fun limit(limit: UInt): Query {
		this.limit = limit
		return this
	}

	override fun iterator(): Iterator<ResultRow> {
		val columnSelections = selectedColumns.joinToString { it.sqlName }
		var query = "SELECT $columnSelections FROM ${table.sqlName} "
		if (limit != null) {
			query += "LIMIT $limit "
			if (skip != null) {
				query += "OFFSET $skip "
			}
		}
		val prepared = connection.prepareAndLog(query.trim())
		val results = prepared.executeQuery()
		return object : Iterator<ResultRow> {
			var hasAdvanced = false
			var hasEnded = false
			override fun hasNext(): Boolean {
				if (hasEnded) return false
				if (hasAdvanced) return true
				if (results.next()) {
					hasAdvanced = true
					return true
				} else {
					hasEnded = true
					return false
				}
			}

			override fun next(): ResultRow {
				if (!hasNext()) {
					throw NoSuchElementException()
				}
				hasAdvanced = false
				return ResultRow(selectedColumns.withIndex().associate {
					it.value to it.value.type.get(results, it.index + 1)
				})
			}

		}
	}
}

class ResultRow(val columnValues: Map<Column<*>, *>) {
	operator fun <T> get(column: Column<T>): T {
		val value = columnValues[column]
			?: error("Invalid column ${column.name}. Only ${columnValues.keys.joinToString { it.name }} are available.")
		return value as T
	}
}




