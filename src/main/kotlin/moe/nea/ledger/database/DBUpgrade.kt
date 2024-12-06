package moe.nea.ledger.database

import java.sql.Connection

interface DBUpgrade {
	val toVersion: Long
	val fromVersion get() = toVersion - 1
	fun performUpgrade(connection: Connection)

	companion object {

		fun performUpgrades(
			connection: Connection,
			upgrades: Iterable<DBUpgrade>,
		) {
			for (upgrade in upgrades) {
				upgrade.performUpgrade(connection)
			}
		}

		fun performUpgradeChain(
			connection: Connection,
			from: Long, to: Long,
			upgrades: Iterable<DBUpgrade>,
			afterEach: (newVersion: Long) -> Unit,
		) {
			val table = buildLookup(upgrades)
			for (version in (from + 1)..(to)) {
				val currentUpgrades = table[version] ?: listOf()
				println("Scheduled ${currentUpgrades.size} upgrades to reach DB version $version")
				performUpgrades(connection, currentUpgrades)
				afterEach(version)
			}
		}

		fun buildLookup(upgrades: Iterable<DBUpgrade>): Map<Long, List<DBUpgrade>> {
			return upgrades.groupBy { it.toVersion }
		}

		fun createTable(to: Long, table: Table, vararg columns: Column<*>): DBUpgrade {
			require(columns.all { it in table.columns })
			return of("Create table ${table}", to) {
				table.createIfNotExists(it, columns.toList())
			}
		}

		fun addColumns(to: Long, table: Table, vararg columns: Column<*>): DBUpgrade {
			return of("Add columns to table $table", to) {
				table.alterTableAddColumns(it, columns.toList())
			}
		}

		fun of(name: String, to: Long, block: (Connection) -> Unit): DBUpgrade {
			return object : DBUpgrade {
				override val toVersion: Long
					get() = to

				override fun performUpgrade(connection: Connection) {
					block(connection)
				}

				override fun toString(): String {
					return name
				}
			}
		}
	}
}