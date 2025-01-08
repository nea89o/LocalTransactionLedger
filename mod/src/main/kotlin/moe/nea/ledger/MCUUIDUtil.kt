package moe.nea.ledger

import com.mojang.util.UUIDTypeAdapter
import net.minecraft.client.Minecraft
import java.util.UUID

object MCUUIDUtil {

	fun parseDashlessUuid(string: String) = UUIDTypeAdapter.fromString(string)
	val NIL_UUID = UUID(0L, 0L)
	fun getPlayerUUID(): UUID {
		val currentUUID = Minecraft.getMinecraft().thePlayer?.uniqueID
			?: Minecraft.getMinecraft().session?.playerID?.let(::parseDashlessUuid)
			?: lastKnownUUID
		lastKnownUUID = currentUUID
		return currentUUID
	}


	private var lastKnownUUID: UUID = NIL_UUID

}