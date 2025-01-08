package moe.nea.ledger.events

import net.minecraftforge.fml.common.eventhandler.Cancelable
import net.minecraftforge.fml.common.eventhandler.Event

@Cancelable
data class TriggerEvent(val action: String) : Event()
