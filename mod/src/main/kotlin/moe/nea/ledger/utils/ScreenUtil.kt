package moe.nea.ledger.utils

import moe.nea.ledger.mixin.AccessorContainerDispenser
import moe.nea.ledger.mixin.AccessorContainerHopper
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.IInventory

object ScreenUtil {
	fun estimateInventory(screen: GuiScreen?): IInventory? {
		if (screen !is GuiContainer) {
			return null
		}
		val container = screen.inventorySlots ?: return null
		if (container is ContainerChest)
			return container.lowerChestInventory
		if (container is AccessorContainerDispenser)
			return container.dispenserInventory_ledger
		if (container is AccessorContainerHopper)
			return container.hopperInventory_ledger
		return null

	}

	fun estimateName(screen: GuiScreen?): String? {
		return estimateInventory(screen)?.name
	}
}