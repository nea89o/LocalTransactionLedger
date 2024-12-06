package moe.nea.ledger.events

import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.fml.common.eventhandler.Event

data class BeforeGuiAction(val gui: GuiScreen) : Event()
