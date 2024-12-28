package moe.nea.ledger.config

import io.github.notenoughupdates.moulconfig.Config
import io.github.notenoughupdates.moulconfig.DescriptionRendereringBehaviour
import io.github.notenoughupdates.moulconfig.annotations.Category
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import moe.nea.ledger.Ledger

class LedgerConfig : Config() {
	override fun getTitle(): String {
		return "§6Ledger §7- §6Hypixel SkyBlock data logger §7by §anea89o"
	}

	override fun saveNow() {
		super.saveNow()
		Ledger.managedConfig.saveToFile()
	}

	override fun getDescriptionBehaviour(option: ProcessedOption?): DescriptionRendereringBehaviour {
		return DescriptionRendereringBehaviour.EXPAND_PANEL
	}

	@Category(name = "Ledger", desc = "")
	@JvmField
	val main = MainOptions()

	@Category(name = "Synchronization", desc = "")
	@JvmField
	val synchronization = SynchronizationOptions()

	@Category(name = "Debug", desc = "")
	@JvmField
	val debug = DebugOptions()

}