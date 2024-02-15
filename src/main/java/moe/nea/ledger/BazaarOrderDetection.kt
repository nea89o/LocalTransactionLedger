package moe.nea.ledger

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class BazaarOrderDetection(val ledger: LedgerLogger, val ids: ItemIdProvider) {
    /*
        [Bazaar] Buy Order Setup! 160x Wheat for 720.0 coins.
        [Bazaar] Claimed 160x Wheat worth 720.0 coins bought for 4.5 each!

        [Bazaar] Sell Offer Setup! 160x Wheat for 933.4 coins.
        [Bazaar] Claimed 34,236,799 coins from selling 176x Hyper Catalyst at 196,741 each!
     */

    val buyOrderClaimed =
        Pattern.compile("\\[Bazaar] Claimed (?<amount>$SHORT_NUMBER_PATTERN)x (?<what>.*) worth (?<coins>$SHORT_NUMBER_PATTERN) coins? bought for $SHORT_NUMBER_PATTERN each!")
    val sellOrderClaimed =
        Pattern.compile("\\[Bazaar] Claimed (?<coins>$SHORT_NUMBER_PATTERN) coins? from selling (?<amount>$SHORT_NUMBER_PATTERN)x (?<what>.*) at $SHORT_NUMBER_PATTERN each!")

    @SubscribeEvent
    fun detectBuyOrders(event: ChatReceived) {
        buyOrderClaimed.useMatcher(event.message) {
            ledger.logEntry(
                LedgerEntry(
                    "BAZAAR_BUY_ORDER",
                    event.timestamp,
                    parseShortNumber(group("coins")),
                    ids.findForName(group("what")),
                    parseShortNumber(group("amount")).toInt(),
                )
            )
        }
        sellOrderClaimed.useMatcher(event.message) {
            ledger.logEntry(
                LedgerEntry(
                    "BAZAAR_SELL_ORDER",
                    event.timestamp,
                    parseShortNumber(group("coins")),
                    ids.findForName(group("what")),
                    parseShortNumber(group("amount")).toInt(),
                )
            )
        }
    }
}
