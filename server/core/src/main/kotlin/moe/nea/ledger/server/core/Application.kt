package moe.nea.ledger.server.core

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import moe.nea.ledger.database.Database
import moe.nea.ledger.server.core.api.Documentation
import moe.nea.ledger.server.core.api.Info
import moe.nea.ledger.server.core.api.apiRouting
import moe.nea.ledger.server.core.api.openApiDocsJson
import java.io.File

fun main(args: Array<String>) {
	EngineMain.main(args)
}


fun Application.module() {
	install(Compression)
	install(Documentation) {
		info = Info(
			"Ledger Analysis Server",
			"Your local API for loading ledger data",
			"TODO: buildconfig"
		)
	}
	install(ContentNegotiation) {
		json(Json {
			this.explicitNulls = false
			this.encodeDefaults = true
		})
//		cbor()
	}
	val database = Database(File(System.getProperty("ledger.databasefolder")))
	database.loadAndUpgrade()
	routing {
		route("/api") {
			this.apiRouting(database)
		}
		route("/api.json") {
			openApiDocsJson()
		}
	}
}

