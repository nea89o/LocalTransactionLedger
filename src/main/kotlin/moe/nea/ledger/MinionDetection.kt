package moe.nea.ledger

import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class MinionDetection(val ledger: LedgerLogger) {
	// §aYou received §r§6367,516.8 coins§r§a!
	val hopperCollectPattern = "You received (?<amount>$SHORT_NUMBER_PATTERN) coins?!".toPattern()
	val minionNamePattern = "(?<name>.*) Minion (?<level>$ROMAN_NUMBER_PATTERN)".toPattern()

	var lastOpenedMinion = ExpiringValue.empty<String>()

	@SubscribeEvent
	fun onBeforeClaim(event: BeforeGuiAction) {
		val container = event.gui as? GuiChest ?: return
		val inv = (container.inventorySlots as ContainerChest).lowerChestInventory
		val invName = inv.displayName.unformattedText.unformattedString()
		minionNamePattern.useMatcher(invName) {
			val name = group("name")
			val level = parseRomanNumber(group("level"))
			lastOpenedMinion = ExpiringValue(name.uppercase().replace(" ", "_") + "_" + level)
		}
	}


	@SubscribeEvent
	fun onChat(event: ChatReceived) {
		hopperCollectPattern.useMatcher(event.message) {
			val minionName = lastOpenedMinion.consume(3.seconds)
			ledger.logEntry(LedgerEntry(
				"AUTOMERCHANT_PROFIT_COLLECT",
				Instant.now(),
				parseShortNumber(group("amount")),
				minionName, // TODO: switch to its own column idk
			))
		}
	}

}