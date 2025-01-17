package moe.nea.ledger.server.core.api

import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import moe.nea.ledger.database.DBLogEntry
import moe.nea.ledger.database.Database
import moe.nea.ledger.server.core.Profile

fun Route.apiRouting(database: Database) {
	get("/") {
		call.respondText("K")
	}
	get("/profiles") {
		val profiles = DBLogEntry.from(database.connection)
			.select(DBLogEntry.playerId, DBLogEntry.profileId)
			.distinct()
			.map {
				Profile(it[DBLogEntry.playerId], it[DBLogEntry.profileId])
			}
		call.respond(profiles)
	}.docs {
		respondsOk {
			schema<List<Profile>>()
		}
	}
}
