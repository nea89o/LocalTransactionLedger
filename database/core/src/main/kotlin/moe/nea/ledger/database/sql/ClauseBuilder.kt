package moe.nea.ledger.database.sql

import moe.nea.ledger.database.Column

class ClauseBuilder {
	fun <T> column(column: Column<T>): Operand<T> = ColumnOperand(column)
	fun string(string: String): StringOperand = StringOperand(string)
	infix fun Operand<*>.eq(operand: Operand<*>) = EqualsClause(this, operand)
	infix fun Operand<*>.like(op: StringOperand) = LikeClause(this, op)
	infix fun Operand<*>.like(op: String) = LikeClause(this, string(op))
}