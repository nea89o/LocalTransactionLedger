package moe.nea.ledger.database

import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.TransactionType
import moe.nea.ledger.database.columns.DBDouble
import moe.nea.ledger.database.columns.DBEnum
import moe.nea.ledger.database.columns.DBString
import moe.nea.ledger.database.columns.DBUlid
import moe.nea.ledger.database.columns.DBUuid

object DBLogEntry : Table("LogEntry") {
	val transactionId = column("transactionId", DBUlid)
	val type = column("type", DBEnum<TransactionType>())
	val profileId = column("profileId", DBUuid)
	val playerId = column("playerId", DBUuid)
}

object DBItemEntry : Table("ItemEntry") {
	val transactionId = column("transactionId", DBUlid) // TODO: add foreign keys
	val mode = column("mode", DBEnum<ItemChange.ChangeDirection>())
	val itemId = column("item", DBString.mapped(ItemId::string, ::ItemId))
	val size = column("size", DBDouble)

	fun objMap(result: ResultRow): ItemChange {
		return ItemChange(
			result[itemId],
			result[size],
			result[mode],
		)
	}
}
