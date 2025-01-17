package moe.nea.ledger.server.aio

import io.ktor.server.application.Application
import io.ktor.server.http.content.singlePageApplication
import io.ktor.server.routing.Routing
import moe.nea.ledger.server.core.AIOProvider


class AIO : AIOProvider {
	override fun Routing.installExtraRouting() {
		singlePageApplication {
			useResources = true
			filesPath = "ledger-web-dist"
			defaultPage = "index.html"
		}
	}

	override fun Application.module() {
	}
}