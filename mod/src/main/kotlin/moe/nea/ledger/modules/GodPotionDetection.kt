package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.gen.ItemIds
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.di.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GodPotionDetection {

    val godPotionDrank = "(SIP|SLURP|GULP|CHUGALUG)! The God Potion grants you powers for .*!".toPattern()

    @Inject
    lateinit var logger: LedgerLogger

    @SubscribeEvent
    fun onChat(event: ChatReceived) {
        godPotionDrank.useMatcher(event.message) {
            logger.logEntry(
                LedgerEntry(
                    TransactionType.GOD_POTION_DRANK,
                    event.timestamp,
                    listOf(
                        ItemChange.lose(ItemIds.GOD_POTION_2, 1)
                    )
                )
            )
        }
    }
}