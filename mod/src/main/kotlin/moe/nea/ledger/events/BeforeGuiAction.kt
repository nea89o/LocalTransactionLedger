package moe.nea.ledger.events

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import moe.nea.ledger.telemetry.GuiContextValue
import moe.nea.ledger.utils.telemetry.ContextValue
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.fml.common.eventhandler.Event

data class BeforeGuiAction(val gui: GuiScreen) : LedgerEvent() {
	val chest = gui as? GuiChest
	val chestSlots = chest?.inventorySlots as ContainerChest?
	override fun serialize(): JsonElement {
		return JsonObject().apply {
			add("gui", GuiContextValue(gui).serialize())
		}
	}
}
