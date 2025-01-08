package moe.nea.ledger.utils

import net.minecraft.client.Minecraft
import java.util.concurrent.Executor

class MinecraftExecutor : Executor {
	override fun execute(command: Runnable) {
		Minecraft.getMinecraft().addScheduledTask(command)
	}
}