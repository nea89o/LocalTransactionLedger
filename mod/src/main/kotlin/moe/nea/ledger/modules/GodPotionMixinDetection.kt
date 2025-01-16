package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.ItemIdProvider
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.di.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GodPotionMixinDetection {

    val godPotionMixinDrank = "SCHLURP! The (effects of the )?(?<what>.*?) (grants you effects|have been extended by) .*! They will pause if your God Potion expires.".toPattern()

    @Inject
    lateinit var logger: LedgerLogger

    @Inject
    lateinit var itemIdProvider: ItemIdProvider

    @SubscribeEvent
    fun onChat(event: ChatReceived) {
        godPotionMixinDrank.useMatcher(event.message) {
            logger.logEntry(
                LedgerEntry(
	                TransactionType.GOD_POTION_MIXIN_DRANK,
	                event.timestamp,
	                listOf(
                        ItemChange.lose(itemIdProvider.findForName(group("what")) ?: ItemId.NIL, 1)
                    )
                )
            )
        }
    }
}