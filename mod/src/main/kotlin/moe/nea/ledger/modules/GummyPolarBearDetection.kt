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

class GummyPolarBearDetection {

    val ateGummyPolarBear = "You ate a Re-heated Gummy Polar Bear!".toPattern()

    @Inject
    lateinit var logger: LedgerLogger

    @SubscribeEvent
    fun onChat(event: ChatReceived) {
        ateGummyPolarBear.useMatcher(event.message) {
            logger.logEntry(
                LedgerEntry(
                    TransactionType.GUMMY_POLAR_BEAR_ATE,
                    event.timestamp,
                    listOf(
                        ItemChange.lose(ItemIds.REHEATED_GUMMY_POLAR_BEAR, 1)
                    )
                )
            )
        }
    }
}