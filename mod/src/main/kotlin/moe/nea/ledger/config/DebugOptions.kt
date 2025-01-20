package moe.nea.ledger.config

import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import moe.nea.ledger.DevUtil

class DebugOptions {
	@ConfigOption(name = "Log entries to chat",
	              desc = "Appends all logged entries into the chat as they are logged. This option does not persist on restarts.")
	@Transient
	@ConfigEditorBoolean
	@JvmField
	var logEntries = DevUtil.isDevEnv
}
