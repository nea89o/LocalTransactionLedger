package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.SHORT_NUMBER_PATTERN
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.gen.ItemIds
import moe.nea.ledger.parseShortNumber
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.di.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Matcher
import java.util.regex.Pattern

class StonksAuctionDetection {

    val stonksPlacedBid =
        Pattern.compile("Successfully placed your Stonk bid of (?<coins>$SHORT_NUMBER_PATTERN) Coins!")
    val stonksIncreaseBid =
        Pattern.compile("Successfully increased your Stonk bid by (?<coins>$SHORT_NUMBER_PATTERN) Coins!")
    val stonksClaim =
        Pattern.compile("You claimed (?<count>$SHORT_NUMBER_PATTERN)x Stock of Stonks from the Stonks Auction!")

    @Inject
    lateinit var logger: LedgerLogger

    @SubscribeEvent
    fun onChat(event: ChatReceived) {
        fun Matcher.logBidding() {
            logger.logEntry(
                LedgerEntry(
                    TransactionType.STONKS_AUCTION,
                    event.timestamp,
                    listOf(
                        ItemChange.loseCoins(parseShortNumber(group("coins"))),
                    )
                )
            )
        }

        fun Matcher.logClaim() {
            logger.logEntry(
                LedgerEntry(
                    TransactionType.STONKS_AUCTION,
                    event.timestamp,
                    listOf(
                        ItemChange.gain(ItemIds.STOCK_OF_STONKS, parseShortNumber(group("count")))
                    )
                )
            )
        }

        stonksPlacedBid.useMatcher(event.message) { logBidding() }
        stonksIncreaseBid.useMatcher(event.message) { logBidding() }
        stonksClaim.useMatcher(event.message) { logClaim() }
    }
}
