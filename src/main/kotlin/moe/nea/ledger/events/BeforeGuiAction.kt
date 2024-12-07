package moe.nea.ledger.events

import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.Event

data class BeforeGuiAction(val gui: GuiScreen) : Event() {
	val chest = gui as? GuiChest
	val chestSlots = chest?.inventorySlots as ContainerChest?
}
