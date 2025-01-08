package moe.nea.ledger.config

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class MainOptions {
	@ConfigOption(name = "Marker for Update UI", desc = "_")
	@JvmField
	@UpdateUiMarker
	@Transient
	var testOption = Unit

	@ConfigOption(name = "Check for Updates", desc = "Automatically check for updates on startup")
	@ConfigEditorDropdown
	@JvmField
	var updateCheck = UpdateCheckBehaviour.SEMI

	enum class UpdateCheckBehaviour(val label: String) {
		SEMI("Semi-Automatic"),
		FULL("Full-Automatic"),
		NONE("Don't check");

		override fun toString(): String {
			return label
		}
	}
}
