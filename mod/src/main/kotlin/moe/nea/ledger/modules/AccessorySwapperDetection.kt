package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.gen.ItemIds
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.di.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class AccessorySwapperDetection {

    val swapperUsed = "Swapped .* enrichments to .*!".toPattern()

    @Inject
    lateinit var logger: LedgerLogger

    @SubscribeEvent
    fun onChat(event: ChatReceived) {
        swapperUsed.useMatcher(event.message) {
            logger.logEntry(
                LedgerEntry(
                    TransactionType.ACCESSORIES_SWAPPING,
                    event.timestamp,
                    listOf(
                        ItemChange.lose(ItemIds.TALISMAN_ENRICHMENT_SWAPPER, 1)
                    )
                )
            )
        }
    }
}