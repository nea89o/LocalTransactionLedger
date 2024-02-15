package moe.nea.ledger

import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class AuctionHouseDetection(val ledger: LedgerLogger, val ids: ItemIdProvider) {
    data class LastViewedItem(
        val count: Int,
        val id: String,
    )
    /*
        You collected 8,712,000 coins from selling Ultimate Carrot Candy Upgrade to [VIP] kodokush in an auction!
        You collected 60,000 coins from selling Walnut to [MVP++] Alea1337 in an auction!
        You purchased 2x Walnut for 69 coins!
        You purchased ◆ Ice Rune I for 4,000 coins!
     */

    val collectSold =
        Pattern.compile("You collected (?<coins>$SHORT_NUMBER_PATTERN) coins? from selling (?<what>.*) to (?<buyer>.*) in an auction!")
    val purchased =
        Pattern.compile("You purchased (?:(?<amount>[0-9]+)x )?(?<what>.*) for (?<coins>$SHORT_NUMBER_PATTERN) coins!")
    var lastViewedItem: LastViewedItem? = null

    @SubscribeEvent
    fun onEvent(event: ChatReceived) {
        collectSold.useMatcher(event.message) {
            ledger.logEntry(
                LedgerEntry(
                    "AUCTION_SOLD",
                    event.timestamp,
                    parseShortNumber(group("coins")),
                    lastViewedItem?.id,
                    lastViewedItem?.count
                )
            )
        }
        purchased.useMatcher(event.message) {
            ledger.logEntry(
                LedgerEntry(
                    "AUCTION_BOUGHT",
                    event.timestamp,
                    parseShortNumber(group("coins")),
                    ids.findForName(group("what")),
                    group("amount")?.let { it.toInt() } ?: 1
                )
            )
        }
    }

    @SubscribeEvent
    fun onBeforeAuctionCollected(event: BeforeGuiAction) {
        // TODO: collect all support
        val chest = (event.gui as? GuiChest) ?: return
        val slots = chest.inventorySlots as ContainerChest
        val name = slots.lowerChestInventory.displayName.unformattedText.unformattedString()

        if (name == "BIN Auction View" || name == "Auction View") {
            handleCollectSingleAuctionView(slots)
        }
    }


    fun handleCollectSingleAuctionView(slots: ContainerChest) {
        val soldItem = slots.lowerChestInventory.getStackInSlot(9 + 4) ?: return
        val id = soldItem.getInternalId() ?: return
        val count = soldItem.stackSize
        lastViewedItem = LastViewedItem(count, id)
    }


}
