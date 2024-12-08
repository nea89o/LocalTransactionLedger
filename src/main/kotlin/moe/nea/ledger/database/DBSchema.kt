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

class Column<T> @Deprecated("Use Table.column instead") constructor(
	val table: Table,
	val name: String,
	val type: DBType<T>
) {
	val sqlName get() = "`$name`"
	val qualifiedSqlName get() = table.sqlName + "." + sqlName
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

	fun from(connection: Connection): Query {
		return Query(connection, mutableListOf(), this)
	}

	fun selectAll(connection: Connection): Query {
		return Query(connection, columns.toMutableList(), this)
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

interface SQLQueryComponent {
	fun asSql(): String

	/**
	 * @return the next writable index (should equal to the amount of `?` in [asSql] + [startIndex])
	 */
	fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int

	companion object {
		fun standalone(sql: String): SQLQueryComponent {
			return object : SQLQueryComponent {
				override fun asSql(): String {
					return sql
				}

				override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
					return startIndex
				}
			}
		}
	}
}

interface BooleanExpression : SQLQueryComponent

data class ORExpression(
	val elements: List<BooleanExpression>
) : BooleanExpression {
	init {
		require(elements.isNotEmpty())
	}

	override fun asSql(): String {
		return (elements + SQLQueryComponent.standalone("FALSE")).joinToString(" OR ", "(", ")") { it.asSql() }
	}

	override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
		var index = startIndex
		for (element in elements) {
			index = element.appendToStatement(stmt, index)
		}
		return index
	}
}


data class ANDExpression(
	val elements: List<BooleanExpression>
) : BooleanExpression {
	init {
		require(elements.isNotEmpty())
	}

	override fun asSql(): String {
		return (elements + SQLQueryComponent.standalone("TRUE")).joinToString(" AND ", "(", ")") { it.asSql() }
	}

	override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
		var index = startIndex
		for (element in elements) {
			index = element.appendToStatement(stmt, index)
		}
		return index
	}
}

class ClauseBuilder {
	fun <T> column(column: Column<T>): Operand<T> = Operand.ColumnOperand(column)
	fun string(string: String): Operand.StringOperand = Operand.StringOperand(string)
	infix fun Operand<*>.eq(operand: Operand<*>) = Clause.EqualsClause(this, operand)
	infix fun Operand<*>.like(op: Operand.StringOperand) = Clause.LikeClause(this, op)
	infix fun Operand<*>.like(op: String) = Clause.LikeClause(this, string(op))
}

interface Clause : BooleanExpression {
	companion object {
		operator fun invoke(builder: ClauseBuilder.() -> Clause): Clause {
			return builder(ClauseBuilder())
		}
	}

	data class EqualsClause(val left: Operand<*>, val right: Operand<*>) : Clause { // TODO: typecheck this somehow
		override fun asSql(): String {
			return left.asSql() + " = " + right.asSql()
		}

		override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
			var index = startIndex
			index = left.appendToStatement(stmt, index)
			index = right.appendToStatement(stmt, index)
			return index
		}
	}

	data class LikeClause<T>(val left: Operand<T>, val right: Operand.StringOperand) : Clause {
		//TODO: check type safety with this one
		override fun asSql(): String {
			return "(" + left.asSql() + " LIKE " + right.asSql() + ")"
		}

		override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
			var index = startIndex
			index = left.appendToStatement(stmt, index)
			index = right.appendToStatement(stmt, index)
			return index
		}
	}
}

interface Operand<T> : SQLQueryComponent {
	data class ColumnOperand<T>(val column: Column<T>) : Operand<T> {
		override fun asSql(): String {
			return column.qualifiedSqlName
		}

		override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
			return startIndex
		}
	}

	data class StringOperand(val value: String) : Operand<String> {
		override fun asSql(): String {
			return "?"
		}

		override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
			stmt.setString(startIndex, value)
			return 1 + startIndex
		}
	}
}

data class Join(
	val table: Table,
//TODO: aliased columns	val tableAlias: String,
	val filter: Clause,
) : SQLQueryComponent {
	//	JOIN ItemEntry on LogEntry.transactionId = ItemEntry.transactionId
	override fun asSql(): String {
		return "JOIN ${table.sqlName} ON ${filter.asSql()}"
	}

	override fun appendToStatement(stmt: PreparedStatement, startIndex: Int): Int {
		return filter.appendToStatement(stmt, startIndex)
	}
}

fun List<SQLQueryComponent>.concatToFilledPreparedStatement(connection: Connection): PreparedStatement {
	var query = ""
	for (element in this) {
		if (query.isNotEmpty()) {
			query += " "
		}
		query += element.asSql()
	}
	val statement = connection.prepareAndLog(query)
	var index = 1
	for (element in this) {
		val nextIndex = element.appendToStatement(statement, index)
		if (nextIndex < index) error("$element went back in time")
		index = nextIndex
	}
	return statement
}

class Query(
	val connection: Connection,
	val selectedColumns: MutableList<Column<*>>,
	var table: Table,
	var limit: UInt? = null,
	var skip: UInt? = null,
	val joins: MutableList<Join> = mutableListOf(),
	val conditions: MutableList<BooleanExpression> = mutableListOf(),
//	var order: OrderClause?= null,
) : Iterable<ResultRow> {
	fun join(table: Table, on: Clause): Query {
		joins.add(Join(table, on))
		return this
	}

	fun where(binOp: BooleanExpression): Query {
		conditions.add(binOp)
		return this
	}

	fun select(vararg columns: Column<*>): Query {
		selectedColumns.addAll(columns)
		return this
	}

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
		val columnSelections = selectedColumns.joinToString { it.qualifiedSqlName }
		val elements = mutableListOf(
			SQLQueryComponent.standalone("SELECT $columnSelections FROM ${table.sqlName}"),
		)
		elements.addAll(joins)
		if (conditions.any()) {
			elements.add(SQLQueryComponent.standalone("WHERE"))
			elements.add(ANDExpression(conditions))
		}
		if (limit != null) {
			elements.add(SQLQueryComponent.standalone("LIMIT $limit"))
			if (skip != null) {
				elements.add(SQLQueryComponent.standalone("OFFSET $skip"))
			}
		}
		val prepared = elements.concatToFilledPreparedStatement(connection)
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
					results.close() // TODO: somehow enforce closing this
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




