package moe.nea.ledger

import io.github.notenoughupdates.moulconfig.common.IMinecraft
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender

class ConfigCommand : CommandBase() {
	override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
		return true
	}

	override fun getCommandName(): String {
		return "ledgerconfig"
	}

	override fun getCommandUsage(sender: ICommandSender?): String {
		return ""
	}

	override fun processCommand(sender: ICommandSender?, args: Array<out String>) {
		val editor = Ledger.managedConfig.getEditor()
		editor.search(args.joinToString(" "))
		Ledger.runLater {
			IMinecraft.instance.openWrappedScreen(editor)
		}
	}

	override fun getCommandAliases(): List<String> {
		return listOf("moneyledger")
	}
}