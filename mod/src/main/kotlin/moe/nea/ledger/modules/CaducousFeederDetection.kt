package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.GuiClickEvent
import moe.nea.ledger.gen.ItemIds
import moe.nea.ledger.getDisplayNameU
import moe.nea.ledger.getInternalId
import moe.nea.ledger.unformattedString
import moe.nea.ledger.utils.di.Inject
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.Instant

class CaducousFeederDetection {

	@Inject
	lateinit var logger: LedgerLogger

	@Inject
	lateinit var minecraft: Minecraft

	@SubscribeEvent
	fun onFeederClick(event: GuiClickEvent) {
		val slot = event.slotIn ?: return
		val displayName = slot.inventory.displayName.unformattedText
		if (!displayName.unformattedString().contains("Confirm Caducous Feeder")) return
		val stack = slot.stack ?: return
		val player = minecraft.thePlayer ?: return
		if (!player.inventory.mainInventory.any { it?.getInternalId() == ItemIds.ULTIMATE_CARROT_CANDY }) return
		if (stack.getDisplayNameU() != "§aUse Caducous Feeder") return
		val petId = slot.inventory.getStackInSlot(13)?.getInternalId() ?: ItemId.NIL

		logger.logEntry(
			LedgerEntry(
				TransactionType.CADUCOUS_FEEDER_USED,
				Instant.now(),
				listOf(
					ItemChange.lose(ItemIds.ULTIMATE_CARROT_CANDY, 1),
					ItemChange(petId, 1.0, ItemChange.ChangeDirection.TRANSFORM),
				)
			)
		)
	}
}