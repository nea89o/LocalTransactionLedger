package moe.nea.ledger

import com.google.gson.JsonObject
import java.time.Instant
import java.util.UUID

data class LedgerEntry(
	val transactionType: TransactionType,
	val timestamp: Instant,
	val items: List<ItemChange>,
) {
	fun intoJson(profileId: UUID?): JsonObject {
		val coinAmount = items.find { it.itemId == ItemId.COINS || it.itemId == ItemId.BITS }?.count
		val nonCoins = items.find { it.itemId != ItemId.COINS && it.itemId != ItemId.BITS }
		return JsonObject().apply {
			addProperty("transactionType", transactionType.name)
			addProperty("timestamp", timestamp.toEpochMilli().toString())
			addProperty("totalTransactionValue", coinAmount)
			addProperty("itemId", nonCoins?.itemId?.string ?: "")
			addProperty("itemAmount", nonCoins?.count ?: 0.0)
			addProperty("profileId", profileId.toString())
			addProperty(
				"playerId",
				UUIDUtil.getPlayerUUID().toString()
			)
		}
	}
}