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
    var lastViewedItems: MutableList<LastViewedItem> = mutableListOf()

    @SubscribeEvent
    fun onEvent(event: ChatReceived) {
        collectSold.useMatcher(event.message) {
            val lastViewedItem = lastViewedItems.removeLastOrNull()
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
                    group("amount")?.toInt() ?: 1
                )
            )
        }
    }

    @SubscribeEvent
    fun onBeforeAuctionCollected(event: BeforeGuiAction) {
        val chest = (event.gui as? GuiChest) ?: return
        val slots = chest.inventorySlots as ContainerChest
        val name = slots.lowerChestInventory.displayName.unformattedText.unformattedString()

        if (name == "BIN Auction View" || name == "Auction View") {
            handleCollectSingleAuctionView(slots)
        }
        if (name == "Manage Auctions") {
            handleCollectMultipleAuctionsView(slots)
        }
    }

    private fun handleCollectMultipleAuctionsView(slots: ContainerChest) {
        lastViewedItems =
            (0 until slots.lowerChestInventory.sizeInventory)
                .mapNotNull { slots.lowerChestInventory.getStackInSlot(it) }
                .filter {
                    it.getLore().contains("§7Status: §aSold!") // BINs
                            || it.getLore().contains("§7Status: §aEnded!") // Auctions
                }
                .mapNotNull { LastViewedItem(it.stackSize, it.getInternalId() ?: return@mapNotNull null) }
                .toMutableList()
    }


    fun handleCollectSingleAuctionView(slots: ContainerChest) {
        val soldItem = slots.lowerChestInventory.getStackInSlot(9 + 4) ?: return
        val id = soldItem.getInternalId() ?: return
        val count = soldItem.stackSize
        lastViewedItems = mutableListOf(LastViewedItem(count, id))
    }


}
