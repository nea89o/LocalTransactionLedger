package moe.nea.ledger

import moe.nea.ledger.mixin.AccessorGuiEditSign
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class BazaarOrderDetection(val ledger: LedgerLogger, val ids: ItemIdProvider) {

    val buyOrderClaimed =
        Pattern.compile("\\[Bazaar] Claimed (?<amount>$SHORT_NUMBER_PATTERN)x (?<what>.*) worth (?<coins>$SHORT_NUMBER_PATTERN) coins? bought for $SHORT_NUMBER_PATTERN each!")
    val sellOrderClaimed =
        Pattern.compile("\\[Bazaar] Claimed (?<coins>$SHORT_NUMBER_PATTERN) coins? from selling (?<amount>$SHORT_NUMBER_PATTERN)x (?<what>.*) at $SHORT_NUMBER_PATTERN each!")
    val orderFlipped =
        Pattern.compile("\\[Bazaar] Order Flipped! (?<amount>$SHORT_NUMBER_PATTERN)x (?<what>.*) for (?<coins>$SHORT_NUMBER_PATTERN) coins? of total expected profit.")
    val previousPricePattern =
        Pattern.compile("(?<price>$SHORT_NUMBER_PATTERN)/u")
    var lastFlippedPreviousPrice = 0.0

    @SubscribeEvent
    fun detectSignFlip(event: GuiScreenEvent.InitGuiEvent) {
        val gui = event.gui
        if (gui !is GuiEditSign) return
        gui as AccessorGuiEditSign
        val text = gui.tileEntity_ledger.signText
        if (text[2].unformattedText != "Previous price:") return
        previousPricePattern.useMatcher(text[3].unformattedText) {
            lastFlippedPreviousPrice = parseShortNumber(group("price"))
        }
    }

    @SubscribeEvent
    fun detectBuyOrders(event: ChatReceived) {
        orderFlipped.useMatcher(event.message) {
            val amount = parseShortNumber(group("amount")).toInt()
            ledger.logEntry(
                LedgerEntry(
                    "BAZAAR_BUY_ORDER",
                    event.timestamp,
                    lastFlippedPreviousPrice * amount,
                    ids.findForName(group("what")),
                    amount,
                )
            )
        }
        buyOrderClaimed.useMatcher(event.message) {
            ledger.logEntry(
                LedgerEntry(
                    "BAZAAR_BUY_ORDER",
                    event.timestamp,
                    parseShortNumber(group("coins")),
                    ids.findForName(group("what")),
                    parseShortNumber(group("amount")).toInt(),
                )
            )
        }
        sellOrderClaimed.useMatcher(event.message) {
            ledger.logEntry(
                LedgerEntry(
                    "BAZAAR_SELL_ORDER",
                    event.timestamp,
                    parseShortNumber(group("coins")),
                    ids.findForName(group("what")),
                    parseShortNumber(group("amount")).toInt(),
                )
            )
        }
    }
}
