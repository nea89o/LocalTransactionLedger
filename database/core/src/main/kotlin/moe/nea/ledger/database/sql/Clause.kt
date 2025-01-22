package moe.nea.ledger.database.sql

interface Clause : BooleanExpression {
	companion object {
		operator fun <T> invoke(builder: ClauseBuilder.() -> T): T {
			return builder(ClauseBuilder())
		}
	}
}