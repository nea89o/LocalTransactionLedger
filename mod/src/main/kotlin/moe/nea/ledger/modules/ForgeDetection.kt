package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.GuiClickEvent
import moe.nea.ledger.getInternalId
import moe.nea.ledger.matches
import moe.nea.ledger.unformattedString
import moe.nea.ledger.utils.di.Inject
import net.minecraft.item.EnumDyeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.Instant

class ForgeDetection {
	val furnaceSlot = 9 + 4
	val furnaceName = "Forge Slot.*".toPattern()

	@SubscribeEvent
	fun onClick(event: GuiClickEvent) {
		val slot = event.slotIn ?: return
		val clickedItem = slot.stack ?: return
		val dyeColor = EnumDyeColor.byMetadata(clickedItem.itemDamage)
		if (clickedItem.displayName.unformattedString() != "Confirm") return
		if (dyeColor == EnumDyeColor.RED) return
		val furnaceSlotName = slot.inventory.getStackInSlot(furnaceSlot)?.displayName?.unformattedString() ?: return
		if (!furnaceName.matches(furnaceSlotName))
			return
		val cl = (0 until slot.inventory.sizeInventory - 9)
			.mapNotNull {
				val stack = slot.inventory.getStackInSlot(it) ?: return@mapNotNull null
				val x = it % 9
				if (x == 4) return@mapNotNull null
				ItemChange(
					stack.getInternalId() ?: return@mapNotNull null,
					stack.stackSize.toDouble(),
					if (x < 4) ItemChange.ChangeDirection.LOST else ItemChange.ChangeDirection.GAINED
				)
			}
		logger.logEntry(LedgerEntry(
			TransactionType.FORGED,
			Instant.now(),
			cl,
		))
	}

	@Inject
	lateinit var logger: LedgerLogger

}