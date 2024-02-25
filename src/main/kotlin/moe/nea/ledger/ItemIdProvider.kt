package moe.nea.ledger

import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ContainerChest
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ItemIdProvider {

    @SubscribeEvent
    fun onMouseInput(event: GuiScreenEvent.MouseInputEvent.Pre) {
        MinecraftForge.EVENT_BUS.post(BeforeGuiAction(event.gui))
    }

    @SubscribeEvent
    fun onKeyInput(event: GuiScreenEvent.KeyboardInputEvent.Pre) {
        MinecraftForge.EVENT_BUS.post(BeforeGuiAction(event.gui))
    }

    private val knownNames = mutableMapOf<String, String>()

    @SubscribeEvent
    fun saveInventoryIds(event: BeforeGuiAction) {
        val chest = (event.gui as? GuiChest) ?: return
        val slots = chest.inventorySlots as ContainerChest
        val chestName = slots.lowerChestInventory.name.unformattedString()
        val isOrderMenu = chestName == "Your Bazaar Orders" || chestName == "Co-op Bazaar Orders"
        slots.inventorySlots.forEach {
            val stack = it.stack ?: return@forEach
            val nbt = stack.tagCompound ?: NBTTagCompound()
            val display = nbt.getCompoundTag("display")
            var name = display.getString("Name").unformattedString()
            if (isOrderMenu)
                name = name.removePrefix("BUY ").removePrefix("SELL ")
            name = name.trim()
            val id = stack.getInternalId()
            if (id != null && name.isNotBlank()) {
                knownNames[name] = id
            }
        }
    }

    fun findForName(name: String): String? {
        return knownNames[name]
    }

}