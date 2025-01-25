package moe.nea.ledger.database.sql

import moe.nea.ledger.database.DBType

interface TypedOperand<T, Raw> : Operand<T, Raw> {
	val dbType: DBType<T, Raw>
}
