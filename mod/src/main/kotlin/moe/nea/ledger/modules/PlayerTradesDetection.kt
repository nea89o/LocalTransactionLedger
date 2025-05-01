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


class PlayerTradesDetection @Inject constructor(val ledger: LedgerLogger, val ids: ItemIdProvider) {
    val getItemsPattern =
        Pattern.compile(" \\+ (?<count>$SHORT_NUMBER_PATTERN)x? (?<what>.*)")
    val loseItemsPattern =
        Pattern.compile(" - (?<count>$SHORT_NUMBER_PATTERN)x? (?<what>.*)")


    @SubscribeEvent
    fun onChat(event: ChatReceived) {
        getItemsPattern.useMatcher(event.message) {
            val itemName = group("what")
            val count = parseShortNumber(group("count"))

            val change = if (itemName == "coins") {
                ItemChange.gainCoins(count)
            } else {
                val itemId = ids.findForName(itemName) ?: ItemId.NIL
                ItemChange.gain(itemId, count.toInt())
            }
            ledger.logEntry(
                LedgerEntry(
                    TransactionType.PLAYER_TRADING,
                    event.timestamp,
                    listOf(change)
                )
            )
        }
        loseItemsPattern.useMatcher(event.message) {
            val itemName = group("what")
            val count = parseShortNumber(group("count"))

            val change = if (itemName == "coins") {
                ItemChange.loseCoins(count)
            } else {
                val itemId = ids.findForName(itemName) ?: ItemId.NIL
                ItemChange.lose(itemId, count.toInt())
            }
            ledger.logEntry(
                LedgerEntry(
                    TransactionType.PLAYER_TRADING,
                    event.timestamp,
                    listOf(change)
                )
            )
        }
    }
}
