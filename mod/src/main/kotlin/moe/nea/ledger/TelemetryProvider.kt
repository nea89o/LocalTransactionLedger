package moe.nea.ledger

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import moe.nea.ledger.gen.BuildConfig
import moe.nea.ledger.utils.di.DI
import moe.nea.ledger.utils.di.DIProvider
import moe.nea.ledger.utils.telemetry.CommonKeys
import moe.nea.ledger.utils.telemetry.ContextValue
import moe.nea.ledger.utils.telemetry.EventRecorder
import moe.nea.ledger.utils.telemetry.JsonElementContext
import moe.nea.ledger.utils.telemetry.LoggingEventRecorder
import moe.nea.ledger.utils.telemetry.Span
import net.minecraft.client.Minecraft
import net.minecraft.util.Session
import net.minecraftforge.fml.common.Loader

object TelemetryProvider {
	fun injectTo(di: DI) {
		di.register(
			EventRecorder::class.java,
			if (DevUtil.isDevEnv) DIProvider.singeleton(LoggingEventRecorder(Ledger.logger, true))
			else DIProvider.singeleton(
				LoggingEventRecorder(Ledger.logger, false)) // TODO: replace with upload to server
		)
	}

	val USER = "minecraft_user"
	val MINECRAFT_VERSION = "minecraft_version"
	val MODS = "mods"

	class MinecraftUser(val session: Session) : ContextValue {
		override fun serialize(): JsonElement {
			val obj = JsonObject()
			obj.addProperty("uuid", session.playerID)
			obj.addProperty("name", session.username)
			return obj
		}
	}

	fun setupDefaultSpan() {
		val sp = Span.current()
		sp.add(USER, MinecraftUser(Minecraft.getMinecraft().session))
		sp.add(MINECRAFT_VERSION, ContextValue.compound(
			"static" to "1.8.9",
			"rt" to Minecraft.getMinecraft().version,
		))
		val mods = JsonArray()
		Loader.instance().activeModList.map {
			val obj = JsonObject()
			obj.addProperty("id", it.modId)
			obj.addProperty("version", it.version)
			obj.addProperty("displayVersion", it.displayVersion)
			obj
		}.forEach(mods::add)
		sp.add(MODS, JsonElementContext(mods))
		sp.add(CommonKeys.VERSION, ContextValue.string(BuildConfig.FULL_VERSION))
		sp.add(CommonKeys.COMMIT_VERSION, ContextValue.string(BuildConfig.GIT_COMMIT))
	}

	fun setupFor(di: DI) {
		injectTo(di)
		setupDefaultSpan()
	}
}