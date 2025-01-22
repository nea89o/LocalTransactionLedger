package moe.nea.ledger.database.sql

interface Operand<T,
		/**
		 * The db sided type (or a rough equivalence).
		 * @see moe.nea.ledger.database.DBType Raw type parameter
		 */
		Raw> : SQLQueryComponent {

}