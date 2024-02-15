package moe.nea.ledger

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class BazaarDetection(val ledger: LedgerLogger, val ids: ItemIdProvider) {

    val instaBuyPattern =
        Pattern.compile("\\[Bazaar\\] Bought (?<count>[0-9]+)x (?<what>.*) for (?<coins>$SHORT_NUMBER_PATTERN) coins!")
    val instaSellPattern =
        Pattern.compile("\\[Bazaar\\] Sold (?<count>[0-9]+)x (?<what>.*) for (?<coins>$SHORT_NUMBER_PATTERN) coins!")


    @SubscribeEvent
    fun onInstSellChat(event: ChatReceived) {
        instaBuyPattern.useMatcher(event.message) {
            ledger.logEntry(
                LedgerEntry(
                    "BAZAAR_BUY_INSTANT",
                    event.timestamp,
                    parseShortNumber(group("coins")),
                    ids.findForName(group("what")),
                    group("count").toInt(),
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
                    group("count").toInt(),
                )
            )
        }
    }
}
