package moe.nea.ledger.database.sql

class ListClause<R>(
	val lhs: Operand<*, R>,
	val list: ListExpression<*, R>,
) : Clause, SQLQueryComponent by SQLQueryComponent.composite(
	lhs, SQLQueryComponent.standalone("IN"), list
)