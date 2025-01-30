package moe.nea.ledger.modules

import moe.nea.ledger.events.InitializationComplete
import moe.nea.ledger.events.SupplyDebugInfo
import moe.nea.ledger.utils.GsonUtil
import moe.nea.ledger.utils.di.Inject
import moe.nea.ledger.utils.network.Request
import moe.nea.ledger.utils.network.RequestUtil
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.CompletableFuture

class ExternalDataProvider @Inject constructor(
	val requestUtil: RequestUtil
) {
	// TODO: Save all the data locally, so in case of a failed request older versions can be used

	fun createAuxillaryDataRequest(path: String): Request {
		return requestUtil.createRequest("https://github.com/nea89o/ledger-auxiliary-data/raw/refs/heads/master/$path")
	}

	private val itemNameFuture: CompletableFuture<Map<String, String>> = CompletableFuture.supplyAsync {
		val request = createAuxillaryDataRequest("data/item_names.json")
		val response = request.execute(requestUtil)
		val nameMap =
			response?.json(GsonUtil.typeToken<Map<String, String>>())
				?: mapOf()
		return@supplyAsync nameMap
	}

	lateinit var itemNames: Map<String, String>

	class DataLoaded(val provider: ExternalDataProvider) : Event()

	@SubscribeEvent
	fun onDebugData(debugInfo: SupplyDebugInfo) {
		debugInfo.record("externalItemsLoaded", itemNameFuture.isDone && !itemNameFuture.isCompletedExceptionally)
	}

	@SubscribeEvent
	fun onInitComplete(event: InitializationComplete) {
		itemNames = itemNameFuture.join()
		MinecraftForge.EVENT_BUS.post(DataLoaded(this))
	}
}