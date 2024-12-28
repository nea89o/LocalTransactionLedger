package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.events.WorldSwitchEvent
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.di.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DragonEyePlacementDetection {
	val eyePlaced = "☬ You placed a Summoning Eye!( Brace yourselves!)? \\(./.\\)".toPattern()
//☬ You placed a Summoning Eye! Brace yourselves! (8/8)
	var eyeCount = 0

	@SubscribeEvent
	fun onWorldSwap(event: WorldSwitchEvent) {
		eyeCount = 0
	}

	@SubscribeEvent
	fun onRetrieveEye(event: ChatReceived) {
		if (event.message == "You recovered a Summoning Eye!") {
			eyeCount--
		}
		eyePlaced.useMatcher(event.message) {
			eyeCount++
		}
		if (event.message == "Your Sleeping Eyes have been awoken by the magic of the Dragon. They are now Remnants of the Eye!") {
			logger.logEntry(LedgerEntry(
				TransactionType.WYRM_EVOKED,
				event.timestamp,
				listOf(
					ItemChange.lose(ItemId.SUMMONING_EYE, eyeCount)
				)
			))
			eyeCount = 0
		}
	}

	@Inject
	lateinit var logger: LedgerLogger
}