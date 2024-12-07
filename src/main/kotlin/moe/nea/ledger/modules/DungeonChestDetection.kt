package moe.nea.ledger.modules

import moe.nea.ledger.ExpiringValue
import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.ItemIdProvider
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.events.ExtraSupplyIdEvent
import moe.nea.ledger.events.GuiClickEvent
import moe.nea.ledger.getDisplayNameU
import moe.nea.ledger.getInternalId
import moe.nea.ledger.getLore
import moe.nea.ledger.unformattedString
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.Inject
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class DungeonChestDetection @Inject constructor(val logger: LedgerLogger) {

	/*{
		id: "minecraft:chest",
		Count: 1b,
		tag: {
			display: {
				Lore: ["§7Purchase this chest to receive the", "§7rewards above. You can only open", "§7one chest per Dungeons run -", "§7choose wisely!", "", "§7Cost", "§625,000 Coins", "§9Dungeon Chest Key", "", "§7§cNOTE: Coins are withdrawn from your", "§cbank if you don't have enough in", "§cyour purse."],
				Name: "§aOpen Reward Chest"
			}
		},
		Damage: 0s
	}

	{
	id: "minecraft:feather",
	Count: 1b,
	tag: {
		overrideMeta: 1b,
		ench: [],
		HideFlags: 254,
		display: {
			Lore: ["§7Consume a §9Kismet Feather §7to reroll", "§7the loot within this chest.", "", "§7You may only use a feather once", "§7per dungeon run.", "", "§eClick to reroll this chest!"],
			Name: "§aReroll Chest"
		},
		AttributeModifiers: []
	},
	Damage: 0s
}
	*/
	@Inject
	lateinit var itemIdProvider: ItemIdProvider

	@SubscribeEvent
	fun onKismetClick(event: GuiClickEvent) {
		val slot = event.slotIn ?: return
		if (!slot.inventory.displayName.unformattedText.unformattedString().endsWith(" Chest")) return
		val stack = slot.stack ?: return
		if (stack.getDisplayNameU() == "§aReroll Chest") {
			logger.logEntry(
				LedgerEntry(
					TransactionType.KISMET_REROLL,
					Instant.now(),
					listOf(
						ItemChange.lose(ItemId.KISMET_FEATHER, 1)
					)
				)
			)
		}
	}

	data class ChestCost(
		val diff: List<ItemChange>,
		val timestamp: Instant,
	)

	var lastOpenedChest = ExpiringValue.empty<ChestCost>()

	@SubscribeEvent
	fun supplyExtraIds(event: ExtraSupplyIdEvent) {
		event.store("Dungeon Chest Key", ItemId("DUNGEON_CHEST_KEY"))
		event.store("Kismet Feather", ItemId("KISMET_FEATHER"))
	}

	@SubscribeEvent
	fun onRewardChestClick(event: GuiClickEvent) {
		val slot = event.slotIn ?: return
		if (!slot.inventory.displayName.unformattedText.unformattedString().endsWith(" Chest")) return
		val stack = slot.stack ?: return
		val name = stack.getDisplayNameU()
		if (name != "§aOpen Reward Chest") return
		val lore = stack.getLore()
		val cost = itemIdProvider.findCostItemsFromSpan(lore)
		val gain = (9..18)
			.mapNotNull { slot.inventory.getStackInSlot(it) }
			.filter { it.item != Item.getItemFromBlock(Blocks.stained_glass_pane) }
			.map {
				it.getInternalId()?.singleItem()
					?: itemIdProvider.findStackableItemByName(it.displayName)
					?: Pair(ItemId.NIL, it.stackSize.toDouble())
			}
		lastOpenedChest = ExpiringValue(ChestCost(
			cost.map { ItemChange.lose(it.first, it.second) }
					+ gain.map { ItemChange.gain(it.first, it.second) },
			Instant.now()
		))
	}

	val rewardMessage = " .* CHEST REWARDS".toPattern()

	@SubscribeEvent
	fun onChatMessage(event: ChatReceived) {
		if (event.message == "You don't have that many coins in the bank!") {
			lastOpenedChest.take()
		}
		rewardMessage.useMatcher(event.message) {
			val chest = lastOpenedChest.consume(3.seconds) ?: return
			logger.logEntry(LedgerEntry(
				TransactionType.DUNGEON_CHEST_OPEN,
				chest.timestamp,
				chest.diff,
			))
		}
	}
}
