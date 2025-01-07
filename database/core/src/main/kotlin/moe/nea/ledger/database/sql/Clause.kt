package moe.nea.ledger.database.sql

interface Clause : BooleanExpression {
	companion object {
		operator fun invoke(builder: ClauseBuilder.() -> Clause): Clause {
			return builder(ClauseBuilder())
		}
	}

}