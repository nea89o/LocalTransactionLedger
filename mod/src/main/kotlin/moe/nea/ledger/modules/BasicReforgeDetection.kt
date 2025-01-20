package moe.nea.ledger.modules

import moe.nea.ledger.ExpiringValue
import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.SHORT_NUMBER_PATTERN
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.events.GuiClickEvent
import moe.nea.ledger.getDisplayNameU
import moe.nea.ledger.getInternalId
import moe.nea.ledger.getLore
import moe.nea.ledger.parseShortNumber
import moe.nea.ledger.unformattedString
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.di.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class BasicReforgeDetection {

	var costPattern = "(?<cost>$SHORT_NUMBER_PATTERN) Coins".toPattern()

	@Inject
	lateinit var logger: LedgerLogger

	data class ReforgeInstance(
		val price: Double,
		val item: ItemId,
	)

	var lastReforge = ExpiringValue.empty<ReforgeInstance>()

	@SubscribeEvent
	fun onReforgeClick(event: GuiClickEvent) {
		val slot = event.slotIn ?: return
		val displayName = slot.inventory.displayName.unformattedText
		if (!displayName.unformattedString().contains("Reforge Item") &&
			!displayName.unformattedString().startsWith("The Hex")
		) return
		val stack = slot.stack ?: return
		val cost = stack.getLore()
			.firstNotNullOfOrNull { costPattern.useMatcher(it.unformattedString()) { parseShortNumber(group("cost")) } }
			?: return

		if (stack.getDisplayNameU() == "§aReforge Item" || stack.getDisplayNameU() == "§aRandom Basic Reforge") {
			lastReforge = ExpiringValue(ReforgeInstance(cost, ItemId.NIL /*TODO: read out item stack that is being reforged to save it as a transformed item!*/))
		}
	}

	val reforgeChatNotification = "You reforged your .* into a .*!".toPattern()

	@SubscribeEvent
	fun onReforgeChat(event: ChatReceived) {
		reforgeChatNotification.useMatcher(event.message) {
			val reforge = lastReforge.get(3.seconds) ?: return
			logger.logEntry(
				LedgerEntry(
					TransactionType.BASIC_REFORGE,
					event.timestamp,
					listOf(
						ItemChange.loseCoins(reforge.price),
						ItemChange(reforge.item, 1.0, ItemChange.ChangeDirection.TRANSFORM)
					)
				)
			)
		}
	}
}