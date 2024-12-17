package moe.nea.ledger

import moe.nea.ledger.events.SupplyDebugInfo
import moe.nea.ledger.utils.di.Inject
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraftforge.common.MinecraftForge

class DebugDataCommand : CommandBase() {

	override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
		return true
	}

	override fun getCommandName(): String {
		return "ledgerdebug"
	}

	override fun getCommandUsage(sender: ICommandSender?): String {
		return ""
	}

	@Inject
	lateinit var logger: LedgerLogger

	override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
		val debugInfo = SupplyDebugInfo()
		MinecraftForge.EVENT_BUS.post(debugInfo)
		logger.printOut("Collected debug info:")
		debugInfo.data.forEach {
			logger.printOut("${it.first}: ${it.second}")
		}
	}
}