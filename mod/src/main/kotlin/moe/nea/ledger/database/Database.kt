package moe.nea.ledger.database

import moe.nea.ledger.Ledger
import moe.nea.ledger.database.columns.DBString
import java.sql.Connection
import java.sql.DriverManager

class Database {
	lateinit var connection: Connection

	object MetaTable : Table("LedgerMeta") {
		val key = column("key", DBString)
		val value = column("value", DBString)

		init {
			unique(key)
		}
	}

	data class MetaKey(val name: String) {
		companion object {
			val DATABASE_VERSION = MetaKey("databaseVersion")
			val LAST_LAUNCH = MetaKey("lastLaunch")
		}
	}

	fun setMetaKey(key: MetaKey, value: String) {
		MetaTable.insert(connection, Table.OnConflict.REPLACE) {
			it[MetaTable.key] = key.name
			it[MetaTable.value] = value
		}
	}

	val databaseVersion: Long = 1

	fun loadAndUpgrade() {
		connection = DriverManager.getConnection("jdbc:sqlite:${Ledger.dataFolder.resolve("database.db")}")
		MetaTable.createIfNotExists(connection)
		val meta = MetaTable.selectAll(connection).associate { MetaKey(it[MetaTable.key]) to it[MetaTable.value] }
		val lastLaunch = meta[MetaKey.LAST_LAUNCH]?.toLong() ?: 0L
		println("Last launch $lastLaunch")
		setMetaKey(MetaKey.LAST_LAUNCH, System.currentTimeMillis().toString())

		val oldVersion = meta[MetaKey.DATABASE_VERSION]?.toLong() ?: -1
		println("Old Database Version: $oldVersion; Current version: $databaseVersion")
		if (oldVersion > databaseVersion)
			error("Outdated software. Database is newer than me!")
		// TODO: create a backup if there is a db version upgrade happening
		DBUpgrade.performUpgradeChain(
			connection, oldVersion, databaseVersion,
			Upgrades().upgrades
		) { version ->
			setMetaKey(MetaKey.DATABASE_VERSION, version.toString())
		}
	}

}