package moe.nea.ledger.events

import net.minecraftforge.fml.common.eventhandler.Event

class SupplyDebugInfo : Event() { // TODO: collect this in the event recorder
	val data = mutableListOf<Pair<String, Any>>()
	fun record(key: String, value: Any) {
		data.add(key to value)
	}
}