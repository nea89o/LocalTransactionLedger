package moe.nea.ledger.modules

import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.events.GuiClickEvent
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.SHORT_NUMBER_PATTERN
import moe.nea.ledger.getInternalId
import moe.nea.ledger.getLore
import moe.nea.ledger.parseShortNumber
import moe.nea.ledger.unformattedString
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.Instant

class BitsShopDetection @Inject constructor(val ledger: LedgerLogger) {


    data class BitShopEntry(
        val id: String,
        val bitPrice: Int,
        val timestamp: Long = System.currentTimeMillis()
    )

    var lastClickedBitShopItem: BitShopEntry? = null
    var bitCostPattern = "(?<cost>$SHORT_NUMBER_PATTERN) Bits".toPattern()

    @SubscribeEvent
    fun recordLastBitPrice(event: GuiClickEvent) {
        val slot = event.slotIn ?: return
        val name = slot.inventory.displayName.unformattedText.unformattedString()
        if (name != "Community Shop" && !name.startsWith("Bits Shop"))
            return
        val stack = slot.stack ?: return
        val id = stack.getInternalId() ?: return
        val bitPrice = stack.getLore()
            .firstNotNullOfOrNull { bitCostPattern.useMatcher(it.unformattedString()) { parseShortNumber(group("cost")).toInt() } }
            ?: return
        lastClickedBitShopItem = BitShopEntry(id, bitPrice)
    }

    @SubscribeEvent
    fun onChat(event: ChatReceived) {
        if (event.message.startsWith("You bought ")) {
            val lastBit = lastClickedBitShopItem ?: return
            if (System.currentTimeMillis() - lastBit.timestamp > 5000) return
            ledger.logEntry(
                LedgerEntry(
                    "COMMUNITY_SHOP_BUY", Instant.now(),
                    lastBit.bitPrice.toDouble(),
                    lastBit.id,
                    1
                )
            )
        }
    }

}