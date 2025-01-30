package moe.nea.ledger.telemetry

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import moe.nea.ledger.utils.ScreenUtil
import moe.nea.ledger.utils.telemetry.ContextValue
import net.minecraft.client.gui.GuiScreen

class GuiContextValue(val gui: GuiScreen) : ContextValue {
	override fun serialize(): JsonElement {
		return JsonObject().apply {
			addProperty("class", gui.javaClass.name)
			addProperty("name", ScreenUtil.estimateName(gui))
		}
	}
}
