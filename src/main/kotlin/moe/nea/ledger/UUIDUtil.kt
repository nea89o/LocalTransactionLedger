package moe.nea.ledger

import com.mojang.util.UUIDTypeAdapter
import io.azam.ulidj.ULID
import net.minecraft.client.Minecraft
import java.time.Instant
import java.util.UUID
import kotlin.random.Random

object UUIDUtil {
	@JvmInline
	value class ULIDWrapper(
		val wrapped: String
	) {
		init {
			require(ULID.isValid(wrapped))
		}
	}

	fun parseDashlessUuid(string: String) = UUIDTypeAdapter.fromString(string)
	val NIL_UUID = UUID(0L, 0L)
	fun getPlayerUUID(): UUID {
		val currentUUID = Minecraft.getMinecraft().thePlayer?.uniqueID
			?: Minecraft.getMinecraft().session?.playerID?.let(::parseDashlessUuid)
			?: lastKnownUUID
		lastKnownUUID = currentUUID
		return currentUUID
	}

	fun createULIDAt(timestamp: Instant): ULIDWrapper {
		return ULIDWrapper(ULID.generate(
			timestamp.toEpochMilli(),
			Random.nextBytes(10)
		))
	}

	private var lastKnownUUID: UUID = NIL_UUID

}