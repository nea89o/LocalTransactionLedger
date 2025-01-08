package moe.nea.ledger.modules

import moe.nea.ledger.ExpiringValue
import moe.nea.ledger.ItemChange
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.events.ExtraSupplyIdEvent
import moe.nea.ledger.events.GuiClickEvent
import moe.nea.ledger.gen.ItemIds
import moe.nea.ledger.getDisplayNameU
import moe.nea.ledger.unformattedString
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.di.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.Instant
import java.util.concurrent.locks.ReentrantLock
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
						ItemChange.lose(ItemIds.KISMET_FEATHER, 1)
					)
				)
			)
		}
	}


	var lastOpenedChest = ExpiringValue.empty<ChestCost>()

	@SubscribeEvent
	fun supplyExtraIds(event: ExtraSupplyIdEvent) {
		event.store("Dungeon Chest Key", ItemIds.DUNGEON_CHEST_KEY)
		event.store("Kismet Feather", ItemIds.KISMET_FEATHER)
	}

	@SubscribeEvent
	fun onRewardChestClick(event: GuiClickEvent) {
		lastOpenedChest = ExpiringValue(scrapeChestReward(event.slotIn ?: return) ?: return)
	}

	class Mutex<T>(defaultValue: T) {
		private var value: T = defaultValue
		val lock = ReentrantLock()

		fun getUnsafeLockedValue(): T {
			if (!lock.isHeldByCurrentThread)
				error("Accessed unsafe locked value, without holding the lock.")
			return value
		}

		fun <R> withLock(func: (T) -> R): R {
			lock.lockInterruptibly()
			try {
				val ret = func(value)
				if (ret === value) {
					error("Please don't smuggle out the locked value. If this is unintentional, please append a `Unit` instruction to the end of your `withLock` call: `.withLock { /* your existing code */; Unit }`.")
				}
				return ret
			} finally {
				lock.unlock()
			}
		}
	}

	val rewardMessage = " (WOOD|GOLD|DIAMOND|EMERALD|OBSIDIAN|BEDROCK) CHEST REWARDS".toPattern()

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
