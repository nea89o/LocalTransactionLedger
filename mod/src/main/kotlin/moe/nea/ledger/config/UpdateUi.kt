package moe.nea.ledger.config

import io.github.notenoughupdates.moulconfig.gui.GuiComponent
import io.github.notenoughupdates.moulconfig.gui.component.TextComponent
import io.github.notenoughupdates.moulconfig.gui.editors.ComponentEditor
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import moe.nea.ledger.Ledger

class UpdateUi(option: ProcessedOption) : ComponentEditor(option) {
	val delegate by lazy {// TODO
		TextComponent("Hier könnte ihre werbung stehen")
	}

	override fun getDelegate(): GuiComponent {
		return delegate
	}
}