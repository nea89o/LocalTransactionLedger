package moe.nea.ledger.server.core

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import moe.nea.ledger.database.Database
import moe.nea.ledger.gen.BuildConfig
import moe.nea.ledger.server.core.api.Documentation
import moe.nea.ledger.server.core.api.Info
import moe.nea.ledger.server.core.api.Server
import moe.nea.ledger.server.core.api.apiRouting
import moe.nea.ledger.server.core.api.openApiDocsJson
import moe.nea.ledger.server.core.api.openApiUi
import moe.nea.ledger.server.core.api.setApiRoot
import java.io.File

fun main(args: Array<String>) {
	EngineMain.main(args)
}

interface AIOProvider {
	fun Routing.installExtraRouting()
	fun Application.module()
}

fun Application.module() {
	val aio = runCatching {
		Class.forName("moe.nea.ledger.server.aio.AIO")
			.newInstance() as AIOProvider
	}.getOrNull()
	aio?.run { module() }
	install(Compression)
	install(Documentation) {
		info = Info(
			"Ledger Analysis Server",
			"Your local API for loading ledger data",
			BuildConfig.VERSION
		)
		servers.add(
			Server("http://localhost:8080/api", "Your Local Server")
		)
	}
	install(ContentNegotiation) {
		json(Json {
			this.explicitNulls = false
			this.encodeDefaults = true
		})
//		cbor()
	}
	install(CORS) {
		anyHost()
	}
	val database = Database(File(System.getProperty("ledger.databasefolder",
													"/home/nea/.local/share/PrismLauncher/instances/Skyblock/.minecraft/money-ledger")))
	database.loadAndUpgrade()
	routing {
		route("/api") {
			setApiRoot()
			get { call.respondRedirect("/openapi/") }
			apiRouting(database)
		}
		route("/api.json") {
			openApiDocsJson()
		}
		route("/openapi") {
			openApiUi("/api.json")
		}
		aio?.run { installExtraRouting() }
	}
}

