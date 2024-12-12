package moe.nea.ledger.modules

import moe.nea.ledger.ExpiringValue
import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.ItemIdProvider
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.SHORT_NUMBER_PATTERN
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.BeforeGuiAction
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.getInternalId
import moe.nea.ledger.getLore
import moe.nea.ledger.parseShortNumber
import moe.nea.ledger.unformattedString
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.Inject
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.seconds

class AuctionHouseDetection @Inject constructor(val ledger: LedgerLogger, val ids: ItemIdProvider) {
	data class LastViewedItem(
		val count: Int,
		val id: ItemId,
	)
	/*
		You collected 8,712,000 coins from selling Ultimate Carrot Candy Upgrade to [VIP] kodokush in an auction!
		You collected 60,000 coins from selling Walnut to [MVP++] Alea1337 in an auction!
		You purchased 2x Walnut for 69 coins!
		You purchased ◆ Ice Rune I for 4,000 coins!
	 */

	val createAuctionScreen = "Confirm( BIN)? Auction".toPattern()
	val auctionCreationCostPattern = "Cost: (?<cost>$SHORT_NUMBER_PATTERN) coins?".toPattern()

	val auctionCreatedChatPattern = "(BIN )?Auction started for .*".toPattern()

	var lastCreationCost: ExpiringValue<Double> = ExpiringValue.empty()

	@SubscribeEvent
	fun onCreateAuctionClick(event: BeforeGuiAction) {
		val slots = event.chestSlots ?: return
		if (!createAuctionScreen.asPredicate().test(slots.lowerChestInventory.name)) return
		val auctionSlot = slots.lowerChestInventory.getStackInSlot(9 + 2) ?: return
		val creationCost = auctionSlot.getLore().firstNotNullOfOrNull {
			auctionCreationCostPattern.useMatcher(it.unformattedString()) { parseShortNumber(group("cost")) }
		}
		if (creationCost != null) {
			lastCreationCost = ExpiringValue(creationCost)
		}
	}

	@SubscribeEvent
	fun onCreateAuctionChat(event: ChatReceived) {
		auctionCreatedChatPattern.useMatcher(event.message) {
			lastCreationCost.consume(3.seconds)?.let { cost ->
				ledger.logEntry(LedgerEntry(
					TransactionType.AUCTION_LISTING_CHARGE,
					event.timestamp,
					listOf(ItemChange.loseCoins(cost))
				))
			}
		}
	}

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
					TransactionType.AUCTION_SOLD,
					event.timestamp,
					listOfNotNull(
						ItemChange.gainCoins(parseShortNumber(group("coins"))),
						lastViewedItem?.let { ItemChange.lose(it.id, it.count) }
					),
				)
			)
		}
		purchased.useMatcher(event.message) {
			ledger.logEntry(
				LedgerEntry(
					TransactionType.AUCTION_BOUGHT,
					event.timestamp,
					listOf(
						ItemChange.loseCoins(parseShortNumber(group("coins"))),
						ItemChange.gain(
							ids.findForName(group("what")) ?: ItemId.NIL,
							group("amount")?.toInt() ?: 1
						)
					)
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