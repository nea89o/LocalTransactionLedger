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
	                "BAZAAR_BUY_INSTANT",
	                event.timestamp,
	                parseShortNumber(group("coins")),
	                ids.findForName(group("what")),
	                parseShortNumber(group("count")).toInt(),
                )
            )
        }
        instaSellPattern.useMatcher(event.message) {
            ledger.logEntry(
                LedgerEntry(
	                "BAZAAR_SELL_INSTANT",
	                event.timestamp,
	                parseShortNumber(group("coins")),
	                ids.findForName(group("what")),
	                parseShortNumber(group("count")).toInt(),
                )
            )
        }
    }
}
