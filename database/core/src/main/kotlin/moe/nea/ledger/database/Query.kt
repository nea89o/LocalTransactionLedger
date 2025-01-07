package moe.nea.ledger.database

import moe.nea.ledger.database.sql.ANDExpression
import moe.nea.ledger.database.sql.BooleanExpression
import moe.nea.ledger.database.sql.Clause
import moe.nea.ledger.database.sql.Join
import moe.nea.ledger.database.sql.SQLQueryComponent
import moe.nea.ledger.database.sql.SQLQueryGenerator.concatToFilledPreparedStatement
import java.sql.Connection

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