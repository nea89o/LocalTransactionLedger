package moe.nea.ledger.database

import moe.nea.ledger.Ledger
import java.sql.DriverManager

object Database {
	val connection = DriverManager.getConnection("jdbc:sqlite:${Ledger.dataFolder.resolve("database.db")}")

	object MetaTable : Table("LedgerMeta") {
		val key = column("key", DBString)
		val value = column("value", DBString)

		init {
			unique(key)
		}
	}

	fun init() {
		MetaTable.createIfNotExists(connection)
		val meta = MetaTable.selectAll(connection).associate { it[MetaTable.key] to it[MetaTable.value] }
		val lastLaunch = meta["lastLaunch"]?.toLong() ?: 0L
		println("Last launch $lastLaunch")
		MetaTable.insert(connection, Table.OnConflict.REPLACE) {
			it[MetaTable.key] = "lastLaunch"
			it[MetaTable.value] = System.currentTimeMillis().toString()
		}
	}

}