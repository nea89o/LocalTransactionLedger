package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.SHORT_NUMBER_PATTERN
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.GuiClickEvent
import moe.nea.ledger.getDisplayNameU
import moe.nea.ledger.getLore
import moe.nea.ledger.parseShortNumber
import moe.nea.ledger.unformattedString
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.di.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.Instant

class BasicReforgeDetection {

    var costPattern = "(?<cost>$SHORT_NUMBER_PATTERN) Coins".toPattern()

    @Inject
    lateinit var logger: LedgerLogger

    @SubscribeEvent
    fun onReforgeClick(event: GuiClickEvent) {
        val slot = event.slotIn ?: return
        val displayName = slot.inventory.displayName.unformattedText
        if (!displayName.unformattedString().contains("Reforge Item") &&
            !displayName.unformattedString().startsWith("The Hex")
        ) return
        val stack = slot.stack ?: return
        val cost = stack.getLore()
            .firstNotNullOfOrNull { costPattern.useMatcher(it.unformattedString()) { parseShortNumber(group("cost")) } }
            ?: return

        if (stack.getDisplayNameU() == "§aReforge Item" || stack.getDisplayNameU() == "§aRandom Basic Reforge") {
            logger.logEntry(
                LedgerEntry(
                    TransactionType.BASIC_REFORGE,
                    Instant.now(),
                    listOf(
                        ItemChange.loseCoins(cost)
                    )
                )
            )
        }
    }
}