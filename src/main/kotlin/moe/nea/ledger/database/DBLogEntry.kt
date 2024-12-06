package moe.nea.ledger.database

import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.TransactionType

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
}
