package moe.nea.ledger.config

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class Synchronization {
	@ConfigOption(name = "Test Option", desc = "Test Description")
	@ConfigEditorBoolean
	@JvmField
	var testOption = false
}