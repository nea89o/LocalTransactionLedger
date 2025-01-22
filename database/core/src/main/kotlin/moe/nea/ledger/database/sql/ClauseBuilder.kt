package moe.nea.ledger.database.sql

import moe.nea.ledger.database.Column

class ClauseBuilder {
	fun <T, R> column(column: Column<T, R>): Operand<T, R> = ColumnOperand(column)
	fun string(string: String): StringOperand = StringOperand(string)
	infix fun <T> Operand<*, T>.eq(operand: Operand<*, T>): Clause = EqualsClause(this, operand)
	infix fun Operand<*, String>.like(op: StringOperand): Clause = LikeClause(this, op)
	infix fun Operand<*, String>.like(op: String): Clause = LikeClause(this, string(op))
	infix fun <T> Operand<*, T>.lt(op: Operand<*, T>): BooleanExpression = LessThanExpression(this, op)
	infix fun <T> Operand<*, T>.le(op: Operand<*, T>): BooleanExpression = LessThanEqualsExpression(this, op)
	infix fun <T> Operand<*, T>.gt(op: Operand<*, T>): BooleanExpression = op lt this
	infix fun <T> Operand<*, T>.ge(op: Operand<*, T>): BooleanExpression = op le this
	infix fun BooleanExpression.and(clause: BooleanExpression): BooleanExpression = ANDExpression(listOf(this, clause))
	infix fun BooleanExpression.or(clause: BooleanExpression): BooleanExpression = ORExpression(listOf(this, clause))
}