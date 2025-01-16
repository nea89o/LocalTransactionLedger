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

class PestRepellentDetection {

    val pestRepellent = "YUM! Pests will now spawn (?<reduction>[2-4])x less while you break crops for the next 60m!".toPattern()

    @Inject
    lateinit var logger: LedgerLogger

    @SubscribeEvent
    fun onChat(event: ChatReceived) {
        pestRepellent.useMatcher(event.message) {
            val reductionAmount = group("reduction")
            if (reductionAmount == "2") {
                logger.logEntry(
                    LedgerEntry(
                        TransactionType.PEST_REPELLENT_USED,
                        event.timestamp,
                        listOf(
                            ItemChange.lose(ItemIds.PEST_REPELLENT, 1),
                        )
                    )
                )
            } else if (reductionAmount == "4"){
                logger.logEntry(
                    LedgerEntry(
                        TransactionType.PEST_REPELLENT_USED,
                        event.timestamp,
                        listOf(
                            ItemChange.lose(ItemIds.PEST_REPELLENT_MAX, 1),
                        )
                    )
                )
            }
        }
    }
}