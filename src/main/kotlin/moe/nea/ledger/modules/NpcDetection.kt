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
import moe.nea.ledger.utils.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class NpcDetection @Inject constructor(val ledger: LedgerLogger, val ids: ItemIdProvider) {

	val npcBuyPattern =
		Pattern.compile("You bought (back )?(?<what>.*?) (x(?<count>$SHORT_NUMBER_PATTERN) )?for (?<coins>$SHORT_NUMBER_PATTERN) Coins?!")
	val npcSellPattern =
		Pattern.compile("You sold (?<what>.*) (x(?<count>$SHORT_NUMBER_PATTERN) )?for (?<coins>$SHORT_NUMBER_PATTERN) Coins?!")

	// TODO: IMPROVE BUYING FROM NPC TO INCLUDE ITEMS OTHER THAN COINS (KUUDRA KEYS ARE CHEAP)

	@SubscribeEvent
	fun onNpcBuy(event: ChatReceived) {
		npcBuyPattern.useMatcher(event.message) {
			ledger.logEntry(
				LedgerEntry(
					TransactionType.NPC_BUY,
					event.timestamp,
					listOf(
						ItemChange.loseCoins(
							parseShortNumber(group("coins")),
						),
						ItemChange.gain(
							ids.findForName(group("what")) ?: ItemId.NIL,
							group("count")?.let(::parseShortNumber) ?: 1,
						)
					)
				)
			)
		}
		npcSellPattern.useMatcher(event.message) {
			ledger.logEntry(
				LedgerEntry(
					TransactionType.NPC_SELL,
					event.timestamp,
					listOf(
						ItemChange.gainCoins(parseShortNumber(group("coins"))),
						ItemChange.lose(
							ids.findForName(group("what")) ?: ItemId.NIL,
							group("count")?.let(::parseShortNumber)?.toInt() ?: 1,
						)
					)
				)
			)
		}
	}
}
