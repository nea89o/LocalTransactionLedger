package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
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

class GhostCoinDropDetection {

    val ghostCoinPattern =
        Pattern.compile("The ghost's death materialized (?<coins>$SHORT_NUMBER_PATTERN) coins from the mists!")

    @Inject
    lateinit var logger: LedgerLogger

    @SubscribeEvent
    fun onGhostCoinDrop(event: ChatReceived) {
        ghostCoinPattern.useMatcher(event.message) {
            logger.logEntry(
                LedgerEntry(
	                // TODO: merge this into a generic mob drop tt
                    TransactionType.GHOST_COIN_DROP,
                    event.timestamp,
                    listOf(
                        ItemChange.gainCoins(parseShortNumber(group("coins"))),
                    )
                )
            )
        }
    }
}
