package moe.nea.ledger.database.sql

import moe.nea.ledger.database.Column

class ClauseBuilder {
	fun <T> column(column: Column<T>): Operand<T> = ColumnOperand(column)
	fun string(string: String): StringOperand = StringOperand(string)
	infix fun Operand<*>.eq(operand: Operand<*>): Clause = EqualsClause(this, operand)
	infix fun Operand<*>.like(op: StringOperand): Clause = LikeClause(this, op)
	infix fun Operand<*>.like(op: String): Clause = LikeClause(this, string(op))
	infix fun Operand<*>.lt(op: Operand<*>): BooleanExpression = LessThanExpression(this, op)
	infix fun Operand<*>.le(op: Operand<*>): BooleanExpression = LessThanEqualsExpression(this, op)
	infix fun Operand<*>.gt(op: Operand<*>): BooleanExpression = op lt this
	infix fun Operand<*>.ge(op: Operand<*>): BooleanExpression = op le this
	infix fun BooleanExpression.and(clause: BooleanExpression): BooleanExpression = ANDExpression(listOf(this, clause))
	infix fun BooleanExpression.or(clause: BooleanExpression): BooleanExpression = ORExpression(listOf(this, clause))
}