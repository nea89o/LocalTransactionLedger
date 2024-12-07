package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.ItemIdProvider
import moe.nea.ledger.getDisplayNameU
import moe.nea.ledger.getInternalId
import moe.nea.ledger.getLore
import moe.nea.ledger.unformattedString
import moe.nea.ledger.utils.Inject
import net.minecraft.init.Blocks
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import java.time.Instant

abstract class ChestDetection {
	data class ChestCost(
		val diff: List<ItemChange>,
		val timestamp: Instant,
	)

	@Inject
	lateinit var itemIdProvider: ItemIdProvider
	fun scrapeChestReward(rewardSlot: Slot): ChestCost? {
		val inventory = rewardSlot.inventory
		if (!inventory.displayName.unformattedText.unformattedString()
				.endsWith(" Chest")
		) return null
		val rewardStack = rewardSlot.stack ?: return null
		val name = rewardStack.getDisplayNameU()
		if (name != "§aOpen Reward Chest") return null
		val lore = rewardStack.getLore()
		val cost = itemIdProvider.findCostItemsFromSpan(lore)
		val gain = (9..18)
			.mapNotNull { inventory.getStackInSlot(it) }
			.filter { it.item != Item.getItemFromBlock(Blocks.stained_glass_pane) }
			.map {
				it.getInternalId()?.withStackSize(it.stackSize)
					?: itemIdProvider.findStackableItemByName(it.displayName)
					?: ItemId.NIL.withStackSize(it.stackSize)
			}
		return ChestCost(
			cost.map { ItemChange.lose(it.first, it.second) } + gain.map { ItemChange.gain(it.first, it.second) },
			Instant.now()
		)
	}

}