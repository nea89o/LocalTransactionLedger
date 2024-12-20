package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.di.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class EyedropsDetection {

    val capsaicinEyedropsUsed = "You applied the eyedrops on the minion and ran out!".toPattern()

    @Inject
    lateinit var logger: LedgerLogger

    @SubscribeEvent
    fun onChat(event: ChatReceived) {
        capsaicinEyedropsUsed.useMatcher(event.message) {
            logger.logEntry(
                LedgerEntry(
                    TransactionType.CAPSAICIN_EYEDROPS_USED,
                    event.timestamp,
                    listOf(
                        ItemChange.lose(ItemId.CAP_EYEDROPS, 1)
                    )
                )
            )
        }
    }
}