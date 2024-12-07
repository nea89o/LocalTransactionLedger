package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.ItemIdProvider
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.BeforeGuiAction
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.getInternalId
import moe.nea.ledger.getLore
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


class KatDetection {
	@Inject
	lateinit var log: LedgerLogger

	@Inject
	lateinit var itemIdProvider: ItemIdProvider

	val giftNameToIdMap = mapOf(
		"flower" to ItemId("KAT_FLOWER"),
		"bouquet" to ItemId("KAT_BOUQUET"),
	)
	val katGift = "\\[NPC\\] Kat: A (?<gift>.*)\\? For me\\? How sweet!".toPattern()

	@SubscribeEvent
	fun onChat(event: ChatReceived) {
		katGift.useMatcher(event.message) {
			val giftName = group("gift")
			val giftId = giftNameToIdMap[giftName]
			log.logEntry(LedgerEntry(
				TransactionType.KAT_TIMESKIP,
				event.timestamp,
				listOf(
					ItemChange.lose(giftId ?: ItemId.NIL, 1)
				)
			))
		}
	}

	val confirmSlot = 9 + 9 + 4
	val petSlot = 9 + 4

	data class PetUpgrade(
		val beforePetId: ItemId,
		val cost: List<Pair<ItemId, Double>>
	)

	var lastPetUpgradeScheduled: PetUpgrade? = null

	@SubscribeEvent
	fun onClick(event: BeforeGuiAction) {
		val slots = event.chestSlots ?: return
		val petItem = slots.lowerChestInventory.getStackInSlot(petSlot) ?: return
		val beforePetId = petItem.getInternalId() ?: return
		val confirmItem = slots.lowerChestInventory.getStackInSlot(confirmSlot) ?: return
		val lore = confirmItem.getLore()
		val cost = itemIdProvider.findCostItemsFromSpan(lore)
		lastPetUpgradeScheduled = PetUpgrade(beforePetId, cost)
	}

	val petUpgradeDialogue = "\\[NPC\\] Kat: I'll get your (?<type>.*) upgraded to (?<tier>.*) in no time!".toPattern()
	fun upgradePetTier(itemId: ItemId): ItemId {
		val str = itemId.string.split(";", limit = 2)
		if (str.size == 2) {
			val (type, tier) = str
			val tierT = tier.toIntOrNull()
			if (tierT != null)
				return ItemId(type + ";" + (tierT + 1))
		}
		return itemId
	}

	@SubscribeEvent
	fun onPetUpgrade(event: ChatReceived) {
		petUpgradeDialogue.useMatcher(event.message) {
			val upgrade = lastPetUpgradeScheduled ?: return
			lastPetUpgradeScheduled = null
			log.logEntry(LedgerEntry(
				TransactionType.KAT_UPGRADE,
				event.timestamp,
				listOf(
					ItemChange.lose(upgrade.beforePetId, 1),
					ItemChange.gain(upgradePetTier(upgrade.beforePetId), 1),
				) + upgrade.cost.map { ItemChange.lose(it.first, it.second) },
			))
		}
	}

}