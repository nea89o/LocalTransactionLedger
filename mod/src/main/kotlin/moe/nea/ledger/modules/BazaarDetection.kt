package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.ItemIdProvider
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.SHORT_NUMBER_PATTERN
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.parseShortNumber
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.di.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class BazaarDetection @Inject constructor(val ledger: LedgerLogger, val ids: ItemIdProvider) {

	val instaBuyPattern =
		Pattern.compile("\\[Bazaar\\] Bought (?<count>$SHORT_NUMBER_PATTERN)x (?<what>.*) for (?<coins>$SHORT_NUMBER_PATTERN) coins!")
	val instaSellPattern =
		Pattern.compile("\\[Bazaar\\] Sold (?<count>$SHORT_NUMBER_PATTERN)x (?<what>.*) for (?<coins>$SHORT_NUMBER_PATTERN) coins!")


	@SubscribeEvent
	fun onInstSellChat(event: ChatReceived) {
		instaBuyPattern.useMatcher(event.message) {
			ledger.logEntry(
				LedgerEntry(
					TransactionType.BAZAAR_BUY_INSTANT,
					event.timestamp,
					listOf(
						ItemChange.loseCoins(parseShortNumber(group("coins"))),
						ItemChange.gain(
							ids.findForName(group("what")) ?: ItemId.NIL,
							parseShortNumber(group("count"))
						)
					)
				)
			)
		}
		instaSellPattern.useMatcher(event.message) {
			ledger.logEntry(
				LedgerEntry(
					TransactionType.BAZAAR_SELL_INSTANT,
					event.timestamp,
					listOf(
						ItemChange.gainCoins(parseShortNumber(group("coins"))),
						ItemChange.lose(
							ids.findForName(group("what")) ?: ItemId.NIL,
							parseShortNumber(group("count"))
						)
					),
				)
			)
		}
	}
}
