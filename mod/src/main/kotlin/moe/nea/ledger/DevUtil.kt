package moe.nea.ledger

import net.minecraft.launchwrapper.Launch

object DevUtil {
	val isDevEnv = Launch.blackboard["fml.deobfuscatedEnvironment"] as Boolean
}