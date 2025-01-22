package moe.nea.ledger.database

class Upgrades {
	val upgrades = mutableListOf<DBUpgrade>()

	fun add(upgrade: DBUpgrade) = upgrades.add(upgrade)

	init {
		add(DBUpgrade.createTable(
			0, DBLogEntry,
			DBLogEntry.type, DBLogEntry.playerId, DBLogEntry.profileId,
			DBLogEntry.transactionId))
		add(DBUpgrade.createTable(
			0, DBItemEntry,
			DBItemEntry.itemId, DBItemEntry.size, DBItemEntry.mode, DBItemEntry.transactionId
		))
	}
}