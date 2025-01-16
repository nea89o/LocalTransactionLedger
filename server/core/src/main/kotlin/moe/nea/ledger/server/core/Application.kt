package moe.nea.ledger.server.core

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import moe.nea.ledger.database.Database
import moe.nea.ledger.server.core.api.apiRouting
import java.io.File

fun main(args: Array<String>) {
	EngineMain.main(args)
}


fun Application.module() {
	install(Compression)
	install(ContentNegotiation) {
		json()
//		cbor()
	}
	val database = Database(File(System.getProperty("ledger.databasefolder")))
	database.loadAndUpgrade()
	routing {
		route("/api") {
			this.apiRouting(database)
		}
	}
}

