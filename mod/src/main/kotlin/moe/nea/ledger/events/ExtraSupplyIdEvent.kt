package moe.nea.ledger.events

import moe.nea.ledger.ItemId
import net.minecraftforge.fml.common.eventhandler.Event

class ExtraSupplyIdEvent(
	private val store: (String, ItemId) -> Unit
) : Event() {
	fun store(name: String, id: ItemId) {
		store.invoke(name, id)
	}
}
