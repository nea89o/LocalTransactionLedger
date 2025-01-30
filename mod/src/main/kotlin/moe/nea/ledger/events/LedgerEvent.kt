package moe.nea.ledger.events

import moe.nea.ledger.Ledger
import moe.nea.ledger.utils.ErrorUtil
import moe.nea.ledger.utils.telemetry.CommonKeys
import moe.nea.ledger.utils.telemetry.ContextValue
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event

abstract class LedgerEvent : Event(), ContextValue {
	fun post() {
		Ledger.leakDI()
			.provide<ErrorUtil>()
			.catch(
				CommonKeys.EVENT_MESSAGE to ContextValue.string("Error during event execution"),
				"event_instance" to this,
				"event_type" to ContextValue.string(javaClass.name)
			) {
				MinecraftForge.EVENT_BUS.post(this)
			}
	}
}