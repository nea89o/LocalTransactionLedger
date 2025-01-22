package moe.nea.ledger.database.sql

import moe.nea.ledger.database.DBType

abstract class TypedOperand<T, Raw> : Operand<T, Raw> {
	abstract val dbType: DBType<T, Raw>
}
