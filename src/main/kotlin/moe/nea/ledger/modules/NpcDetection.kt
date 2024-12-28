package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.ItemIdProvider
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.SHORT_NUMBER_PATTERN
import moe.nea.ledger.TransactionType
import moe.nea.ledger.asIterable
import moe.nea.ledger.events.BeforeGuiAction
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.events.ExtraSupplyIdEvent
import moe.nea.ledger.getDisplayNameU
import moe.nea.ledger.getInternalId
import moe.nea.ledger.getLore
import moe.nea.ledger.parseShortNumber
import moe.nea.ledger.unformattedString
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.ErrorUtil
import moe.nea.ledger.utils.di.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class NpcDetection @Inject constructor(val ledger: LedgerLogger, val ids: ItemIdProvider) {

	val npcBuyPattern =
		Pattern.compile("You bought (back )?(?<what>.*?) (x(?<count>$SHORT_NUMBER_PATTERN) )?for (?<coins>$SHORT_NUMBER_PATTERN) Coins?!")
	val npcSellPattern =
		Pattern.compile("You sold (?<what>.*) (x(?<count>$SHORT_NUMBER_PATTERN) )?for (?<coins>$SHORT_NUMBER_PATTERN) Coins?!")

	// You bought InfiniDirt™ Wand!
	// You bought Prismapump x4!
	val npcBuyWithItemPattern =
		"You bought (?<what>.*?)!".toPattern()
	var storedPurchases = mutableMapOf<String, List<ItemChange>>()

	@SubscribeEvent
	fun onClick(event: BeforeGuiAction) {
		(event.chestSlots?.lowerChestInventory?.asIterable() ?: listOf())
			.filterNotNull().forEach {
				val name = it.getDisplayNameU().unformattedString()
				val id = it.getInternalId() ?: return@forEach
				val count = it.stackSize
				val cost = ids.findCostItemsFromSpan(it.getLore())
				storedPurchases[name] = listOf(ItemChange.gain(id, count)) + cost.map { ItemChange.unpairLose(it) }
			}
	}

	@SubscribeEvent
	fun addChocolate(event: ExtraSupplyIdEvent) {
		event.store("Chocolate", ItemId("SKYBLOCK_CHOCOLATE"))
	}

	@Inject
	lateinit var errorUtil: ErrorUtil

	@SubscribeEvent
	fun onBarteredItemBought(event: ChatReceived) {
		npcBuyWithItemPattern.useMatcher(event.message) {
			val changes = storedPurchases[group("what")]
			if (changes == null) {
				errorUtil.reportAdHoc("Item bought for items without associated cost")
			}
			storedPurchases.clear()
			ledger.logEntry(
				LedgerEntry(
					TransactionType.NPC_BUY,
					event.timestamp,
					changes ?: listOf()
				)
			)
		}
	}

	@SubscribeEvent
	fun onNpcBuy(event: ChatReceived) {
		npcBuyPattern.useMatcher(event.message) {
			ledger.logEntry(
				LedgerEntry(
					TransactionType.NPC_BUY,
					event.timestamp,
					listOf(
						ItemChange.loseCoins(
							parseShortNumber(group("coins")),
						),
						ItemChange.gain(
							ids.findForName(group("what")) ?: ItemId.NIL,
							group("count")?.let(::parseShortNumber) ?: 1,
						)
					)
				)
			)
		}
		npcSellPattern.useMatcher(event.message) {
			ledger.logEntry(
				LedgerEntry(
					TransactionType.NPC_SELL,
					event.timestamp,
					listOf(
						ItemChange.gainCoins(parseShortNumber(group("coins"))),
						ItemChange.lose(
							ids.findForName(group("what")) ?: ItemId.NIL,
							group("count")?.let(::parseShortNumber)?.toInt() ?: 1,
						)
					)
				)
			)
		}
	}
}
