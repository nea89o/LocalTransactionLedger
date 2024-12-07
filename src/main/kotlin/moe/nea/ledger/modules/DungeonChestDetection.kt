package moe.nea.ledger.modules

import moe.nea.ledger.ExpiringValue
import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.events.ExtraSupplyIdEvent
import moe.nea.ledger.events.GuiClickEvent
import moe.nea.ledger.getDisplayNameU
import moe.nea.ledger.unformattedString
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class DungeonChestDetection @Inject constructor(val logger: LedgerLogger) : ChestDetection() {

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


	var lastOpenedChest = ExpiringValue.empty<ChestCost>()

	@SubscribeEvent
	fun supplyExtraIds(event: ExtraSupplyIdEvent) {
		event.store("Dungeon Chest Key", ItemId("DUNGEON_CHEST_KEY"))
		event.store("Kismet Feather", ItemId("KISMET_FEATHER"))
	}

	@SubscribeEvent
	fun onRewardChestClick(event: GuiClickEvent) {
		lastOpenedChest = ExpiringValue(scrapeChestReward(event.slotIn ?: return) ?: return)
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
