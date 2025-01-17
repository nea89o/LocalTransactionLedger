package moe.nea.ledger.server.core.api

import io.ktor.http.Url
import io.ktor.http.toURI
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import moe.nea.ledger.database.DBLogEntry
import moe.nea.ledger.database.Database
import moe.nea.ledger.server.core.Profile
import sh.ondr.jsonschema.jsonSchema

fun Route.apiRouting(database: Database) {
	get("/profiles") {
		val profiles = DBLogEntry.from(database.connection)
			.select(DBLogEntry.playerId, DBLogEntry.profileId)
			.distinct()
			.map {
				Profile(it[DBLogEntry.playerId], it[DBLogEntry.profileId])
			}
		call.respond(profiles)
	}.docs {
		summary = "List all profiles and players known to ledger"
		operationId = "listProfiles"
		tag(Tags.PROFILE)
		respondsOk {
			schema<List<Profile>>()
		}
	}
	@OptIn(DelicateCoroutinesApi::class)
	val itemNames = GlobalScope.async {
		val itemNamesUrl =
			Url("https://github.com/nea89o/ledger-auxiliary-data/raw/refs/heads/master/data/item_names.json")
		Json.decodeFromStream<Map<String, String>>(itemNamesUrl.toURI().toURL().openStream())
	}
	get("/item") {
		val itemIds = call.queryParameters.getAll("itemId")?.toSet() ?: emptySet()
		val itemNameMap = itemNames.await()
		call.respond(itemIds.associateWith { itemNameMap[it] })
	}.docs {
		summary = "Get item names for item ids"
		operationId = "getItemNames"
		tag(Tags.HYPIXEL)
		queryParameter<List<String>>("itemId")
		respondsOk {
			schema<Map<String, String?>>()
		}
	}
}

enum class Tags : IntoTag {
	PROFILE,
	HYPIXEL,
	MANAGEMENT,
	;

	override fun intoTag(): String {
		return name
	}
}