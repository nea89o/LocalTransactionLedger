package moe.nea.ledger

import moe.nea.ledger.events.TriggerEvent
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.event.ClickEvent
import net.minecraft.util.ChatComponentText
import net.minecraftforge.common.MinecraftForge

class TriggerCommand : CommandBase() {
	fun getTriggerCommandLine(trigger: String): ClickEvent {
		return ClickEvent(ClickEvent.Action.RUN_COMMAND, "/${commandName} $trigger")
	}

	override fun getCommandName(): String {
		return "__ledgertrigger"
	}

	override fun getCommandUsage(sender: ICommandSender?): String {
		return ""
	}

	override fun processCommand(sender: ICommandSender, args: Array<out String>) {
		val event = TriggerEvent(args.joinToString(" "))
		MinecraftForge.EVENT_BUS.post(event)
		if (!event.isCanceled)
			sender.addChatMessage(ChatComponentText("§cCould not find the given trigger. This is an internal command for ledger."))
	}

	override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
		return true
	}

}