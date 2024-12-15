package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.ItemIdProvider
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.SHORT_NUMBER_PATTERN
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.ExtraSupplyIdEvent
import moe.nea.ledger.events.GuiClickEvent
import moe.nea.ledger.getDisplayNameU
import moe.nea.ledger.getLore
import moe.nea.ledger.parseShortNumber
import moe.nea.ledger.unformattedString
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.di.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.Instant

class VisitorDetection {
	@Inject
	lateinit var logger: LedgerLogger

	@Inject
	lateinit var idProvider: ItemIdProvider

	@SubscribeEvent
	fun parseFromItem(event: GuiClickEvent) {
		val stack = event.slotIn?.stack ?: return

		val displayName = stack.getDisplayNameU()
		if (displayName != "§aAccept Offer") return
		val lore = stack.getLore()
		if (!lore.contains("§eClick to give!")) return

		val rewards = lore
			.asSequence()
			.dropWhile { it != "§7Rewards:" }.drop(1)
			.takeWhile { it != "" }
			.mapNotNull { parseGardenLoreLine(it) }
			.map { ItemChange.gain(it.first, it.second) }
			.toList()

		val cost = lore
			.asSequence()
			.dropWhile { it != "§7Items Required:" }.drop(1)
			.takeWhile { it != "" }
			.mapNotNull { parseGardenLoreLine(it) }
			.map { ItemChange.lose(it.first, it.second) }
			.toList()

		logger.logEntry(LedgerEntry(
			TransactionType.VISITOR_BARGAIN,
			Instant.now(),
			cost + rewards
		))
	}

	private fun parseGardenLoreLine(rewardLine: String): Pair<ItemId, Double>? {
		val f = rewardLine.unformattedString().trim()
		return parseSpecialReward(f)
			?: idProvider.findStackableItemByName(f, true)
	}

	private val specialRewardRegex = "\\+(?<amount>${SHORT_NUMBER_PATTERN})x? (?<what>.*)".toPattern()

	private fun parseSpecialReward(specialLine: String): Pair<ItemId, Double>? {
		specialRewardRegex.useMatcher(specialLine) {
			val id = when (group("what")) {
				"Copper" -> ItemId.COPPER
				"Bits" -> ItemId.BITS
				"Garden Experience" -> ItemId.GARDEN
				"Farming XP" -> ItemId.FARMING
				"Gold Essence" -> ItemId.GOLD_ESSENCE
				"Gemstone Powder" -> ItemId.GEMSTONE_POWDER
				"Mithril Powder" -> ItemId.MITHRIL_POWDER
				"Pelts" -> ItemId.PELT
				"Fine Flour" -> ItemId.FINE_FLOUR
				else -> ItemId.NIL
			}
			return Pair(id, parseShortNumber(group("amount")))
		}
		return null
	}


	@SubscribeEvent
	fun supplyNames(event: ExtraSupplyIdEvent) {
		event.store("Carrot", ItemId("CARROT_ITEM"))
		event.store("Potato", ItemId("POTATO_ITEM"))
		event.store("Jacob's Ticket", ItemId("JACOBS_TICKET"))
		event.store("Cocoa Beans", ItemId("INK_SACK:3"))
		event.store("Enchanted Cocoa Beans", ItemId("ENCHANTED_COCOA"))
		event.store("Enchanted Red Mushroom Block", ItemId("ENCHANTED_HUGE_MUSHROOM_2"))
		event.store("Enchanted Brown Mushroom Block", ItemId("ENCHANTED_HUGE_MUSHROOM_1"))
		event.store("Nether Wart", ItemId("NETHER_STALK"))
		event.store("Enchanted Nether Wart", ItemId("ENCHANTED_NETHER_STALK"))
		event.store("Mutant Nether Wart", ItemId("MUTANT_NETHER_STALK"))
		event.store("Jack o' Lantern", ItemId("JACK_O_LANTERN"))
		event.store("Cactus Green", ItemId("INK_SACK:2"))
		event.store("Hay Bale", ItemId("HAY_BLOCK"))
		event.store("Rabbit's Foot", ItemId("RABBIT_FOOT"))
		event.store("Raw Porkchop", ItemId("PORK"))
		event.store("Raw Rabbit", ItemId("RABBIT"))
		event.store("White Wool", ItemId("WOOL"))
		event.store("Copper Dye", ItemId("DYE_COPPER"))
	}
}