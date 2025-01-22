package moe.nea.ledger.server.core.api

import io.ktor.http.Url
import io.ktor.http.toURI
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import moe.nea.ledger.ItemChange
import moe.nea.ledger.TransactionType
import moe.nea.ledger.analysis.Analysis
import moe.nea.ledger.analysis.AnalysisFilter
import moe.nea.ledger.analysis.AnalysisResult
import moe.nea.ledger.database.DBItemEntry
import moe.nea.ledger.database.DBLogEntry
import moe.nea.ledger.database.Database
import moe.nea.ledger.database.sql.Clause
import moe.nea.ledger.server.core.Profile
import moe.nea.ledger.utils.ULIDWrapper
import java.time.Instant
import java.util.ServiceLoader
import java.util.UUID

fun Route.apiRouting(database: Database) {
	val allOfferedAnalysisServices: Map<String, Analysis> = run {
		val serviceLoader = ServiceLoader.load(Analysis::class.java, environment.classLoader)
		val map = mutableMapOf<String, Analysis>()
		serviceLoader.forEach {
			map[it.id] = it
		}
		map
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
	get("/analysis/execute") {
		val analysis = allOfferedAnalysisServices[call.queryParameters["analysis"]] ?: TODO()
		val start = call.queryParameters["tStart"]?.toLongOrNull()?.let { Instant.ofEpochMilli(it) } ?: TODO()
		val end = call.queryParameters["tEnd"]?.toLongOrNull()?.let { Instant.ofEpochMilli(it) } ?: TODO()
		val analysisResult = withContext(Dispatchers.IO) {
			analysis.perform(
				database.connection,
				object : AnalysisFilter {
					override val startWindow: Instant
						get() = start
					override val endWindow: Instant
						get() = end
					override val profiles: List<UUID>
						get() = listOf()
				}
			)
		}
		call.respond(analysisResult)
	}.docs {
		summary = "Execute an analysis on a given timeframe"
		operationId = "executeAnalysis"
		queryParameter<String>("analysis", description = "An analysis id obtained from getAnalysis")
		queryParameter<Long>("tStart", description = "The start of the timeframe to analyze")
		queryParameter<Long>("tEnd",
		                     description = "The end of the timeframe to analyze. Make sure to use the end of the day if you want the entire day included.")
		tag(Tags.DATA)
		respondsOk {
			schema<AnalysisResult>()
		}
	}
	get("/analysis/list") {
		call.respond(allOfferedAnalysisServices.values.map {
			AnalysisListing(it.name, it.id)
		})
	}.docs {
		summary = "List all installed analysis"
		operationId = "getAnalysis"
		tag(Tags.DATA)
		respondsOk {
			schema<List<AnalysisListing>>()
		}
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
	get("/entries") {
		val logs = mutableMapOf<ULIDWrapper, LogEntry>()
		val items = mutableMapOf<ULIDWrapper, MutableList<SerializableItemChange>>()
		withContext(Dispatchers.IO) {
			DBLogEntry.from(database.connection)
				.join(DBItemEntry, Clause { column(DBItemEntry.transactionId) eq column(DBLogEntry.transactionId) })
				.select(DBLogEntry.profileId,
				        DBLogEntry.playerId,
				        DBLogEntry.transactionId,
				        DBLogEntry.type,
				        DBItemEntry.mode,
				        DBItemEntry.itemId,
				        DBItemEntry.size)
				.forEach { row ->
					logs.getOrPut(row[DBLogEntry.transactionId]) {
						LogEntry(row[DBLogEntry.type],
						         row[DBLogEntry.transactionId],
						         listOf())
					}
					items.getOrPut(row[DBLogEntry.transactionId]) { mutableListOf() }
						.add(SerializableItemChange(
							row[DBItemEntry.itemId].string,
							row[DBItemEntry.mode],
							row[DBItemEntry.size],
						))
				}
		}
		val compiled = logs.values.map { it.copy(items = items[it.id]!!) }
		call.respond(compiled)
	}.docs {
		summary = "Get all log entries"
		operationId = "getLogEntries"
		tag(Tags.DATA)
		respondsOk {
			schema<List<LogEntry>>()
		}
	}
}

@Serializable
data class AnalysisListing(
	val name: String,
	val id: String,
)

@Serializable
data class LogEntry(
	val type: TransactionType,
	val id: @Serializable(ULIDSerializer::class) ULIDWrapper,
	val items: List<SerializableItemChange>,
)

@Serializable
data class SerializableItemChange(
	val itemId: String,
	val direction: ItemChange.ChangeDirection,
	val amount: Double,
)

object ULIDSerializer : KSerializer<ULIDWrapper> {
	override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ULID", PrimitiveKind.STRING)

	override fun deserialize(decoder: Decoder): ULIDWrapper {
		return ULIDWrapper(decoder.decodeString())
	}

	override fun serialize(encoder: Encoder, value: ULIDWrapper) {
		encoder.encodeString(value.wrapped)
	}
}

enum class Tags : IntoTag {
	PROFILE,
	HYPIXEL,
	MANAGEMENT,
	DATA,
	;

	override fun intoTag(): String {
		return name
	}
}