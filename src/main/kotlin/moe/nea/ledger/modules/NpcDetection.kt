package moe.nea.ledger.modules

import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.ItemIdProvider
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.SHORT_NUMBER_PATTERN
import moe.nea.ledger.parseShortNumber
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class NpcDetection @Inject constructor(val ledger: LedgerLogger, val ids: ItemIdProvider) {

	val npcBuyPattern =
		Pattern.compile("You bought (back )?(?<what>.*?) (x(?<count>$SHORT_NUMBER_PATTERN) )?for (?<coins>$SHORT_NUMBER_PATTERN) Coins!")
	val npcSellPattern =
		Pattern.compile("You sold (?<what>.*) (x(?<count>$SHORT_NUMBER_PATTERN) )?for (?<coins>$SHORT_NUMBER_PATTERN) Coins!")

	@SubscribeEvent
	fun onNpcBuy(event: ChatReceived) {
		npcBuyPattern.useMatcher(event.message) {
			ledger.logEntry(
				LedgerEntry(
					"NPC_BUY",
					event.timestamp,
					parseShortNumber(group("coins")),
					ids.findForName(group("what")),
					group("count")?.let(::parseShortNumber)?.toInt() ?: 1,
				)
			)
		}
		npcSellPattern.useMatcher(event.message) {
			ledger.logEntry(
				LedgerEntry(
					"NPC_SELL",
					event.timestamp,
					parseShortNumber(group("coins")),
					ids.findForName(group("what")),
					group("count")?.let(::parseShortNumber)?.toInt() ?: 1,
				)
			)
		}
	}
}
