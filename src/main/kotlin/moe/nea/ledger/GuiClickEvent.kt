package moe.nea.ledger

import net.minecraft.inventory.Slot
import net.minecraftforge.fml.common.eventhandler.Event

data class GuiClickEvent(
    val slotIn: Slot?, val slotId: Int, val clickedButton: Int, val clickType: Int
) : Event() {
}