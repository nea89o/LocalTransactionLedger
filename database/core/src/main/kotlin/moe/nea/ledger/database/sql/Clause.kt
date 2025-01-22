package moe.nea.ledger.database.sql

/**
 * Directly constructing [clauses][Clause] is discouraged. Instead [Clause.invoke] should be used.
 */
interface Clause : BooleanExpression {
	companion object {
		operator fun <T> invoke(builder: ClauseBuilder.() -> T): T {
			return builder(ClauseBuilder())
		}
	}
}