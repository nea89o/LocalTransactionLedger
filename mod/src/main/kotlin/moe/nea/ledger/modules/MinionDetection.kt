package moe.nea.ledger.modules

import moe.nea.ledger.ExpiringValue
import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.ROMAN_NUMBER_PATTERN
import moe.nea.ledger.SHORT_NUMBER_PATTERN
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.BeforeGuiAction
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.parseRomanNumber
import moe.nea.ledger.parseShortNumber
import moe.nea.ledger.unformattedString
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.di.Inject
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class MinionDetection @Inject constructor(val ledger: LedgerLogger) {
	// §aYou received §r§6367,516.8 coins§r§a!
	val hopperCollectPattern = "You received (?<amount>$SHORT_NUMBER_PATTERN) coins?!".toPattern()
	val minionNamePattern = "(?<name>.*) Minion (?<level>$ROMAN_NUMBER_PATTERN)".toPattern()

	var lastOpenedMinion = ExpiringValue.empty<ItemId>()

	@SubscribeEvent
	fun onBeforeClaim(event: BeforeGuiAction) {
		val container = event.gui as? GuiChest ?: return
		val inv = (container.inventorySlots as ContainerChest).lowerChestInventory
		val invName = inv.displayName.unformattedText.unformattedString()
		minionNamePattern.useMatcher(invName) {
			val name = group("name")
			val level = parseRomanNumber(group("level"))
			lastOpenedMinion = ExpiringValue(
				ItemId(name.uppercase().replace(" ", "_")
					       .replace("MINION", "GENERATOR") + "_" + level))
		}
	}


	@SubscribeEvent
	fun onChat(event: ChatReceived) {
		hopperCollectPattern.useMatcher(event.message) {
			val minionName = lastOpenedMinion.consume(3.seconds)
			ledger.logEntry(LedgerEntry(
				TransactionType.AUTOMERCHANT_PROFIT_COLLECT,
				Instant.now(),
				listOf(
					ItemChange.gainCoins(parseShortNumber(group("amount"))),
					ItemChange(minionName ?: ItemId.NIL, 1.0, ItemChange.ChangeDirection.CATALYST)
				),
			))
		}
	}

}