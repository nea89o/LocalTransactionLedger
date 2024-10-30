package moe.nea.ledger

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class NpcDetection(val ledger: LedgerLogger, val ids: ItemIdProvider) {

    val npcBuyPattern =
        Pattern.compile("You bought (?<what>.*?) x(?<count>$SHORT_NUMBER_PATTERN) for (?<coins>$SHORT_NUMBER_PATTERN) Coins!")
    val npcSellPattern =
        Pattern.compile("You sold (?<what>.*) x(?<count>$SHORT_NUMBER_PATTERN) for (?<coins>$SHORT_NUMBER_PATTERN) Coins!")

    // You bought Cactus x32 for 465.6 Coins!
    // You sold Cactus x1 for 3 Coins!


    @SubscribeEvent
    fun onNpcBuy(event: ChatReceived) {
        npcBuyPattern.useMatcher(event.message) {
            ledger.logEntry(
                LedgerEntry(
                    "NPC_BUY",
                    event.timestamp,
                    parseShortNumber(group("coins")),
                    ids.findForName(group("what")),
                    parseShortNumber(group("count")).toInt(),
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
                    parseShortNumber(group("count")).toInt(),
                )
            )
        }
    }
}
