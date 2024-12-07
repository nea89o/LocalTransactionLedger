package moe.nea.ledger

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import moe.nea.ledger.database.DBItemEntry
import moe.nea.ledger.database.DBLogEntry
import moe.nea.ledger.database.Database
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.utils.Inject
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.UUID

class LedgerLogger {
	fun printOut(text: String) {
		Minecraft.getMinecraft().ingameGUI?.chatGUI?.printChatMessage(ChatComponentText(text))
	}

	val profileIdPattern =
		"Profile ID: (?<profile>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})".toPattern()

	var currentProfile: UUID? = null

	var shouldLog by Ledger.managedConfig.instance.debug::logEntries

	@Inject
	lateinit var database: Database

	@SubscribeEvent
	fun onProfileSwitch(event: ChatReceived) {
		profileIdPattern.useMatcher(event.message) {
			currentProfile = UUID.fromString(group("profile"))
		}
	}


	fun printToChat(entry: LedgerEntry) {
		val items = entry.items.joinToString("\n§e") { " - ${it.direction} ${it.count}x ${it.itemId}" }
		printOut(
			"""
            §e================= TRANSACTION START
            §eTYPE: §a${entry.transactionType}
            §eTIMESTAMP: §a${entry.timestamp}
            §e%s
            §ePROFILE: §a${currentProfile}
            §e================= TRANSACTION END
            """.trimIndent().replace("%s", items)
		)
	}

	val entries = JsonArray()
	var hasRecentlyMerged = false
	var lastMergeTime = System.currentTimeMillis()

	fun doMerge() {
		val allFiles = folder.listFiles()?.toList() ?: emptyList()
		val mergedJson = allFiles
			.filter { it.name != "merged.json" && it.extension == "json" }
			.sortedDescending()
			.map { it.readText().trim().removePrefix("[").removeSuffix("]") }
			.joinToString(",", "[", "]")
		folder.resolve("merged.json").writeText(mergedJson)
		hasRecentlyMerged = true
	}

	init {
		Runtime.getRuntime().addShutdownHook(Thread { doMerge() })
	}

	@SubscribeEvent
	fun onTick(event: ClientTickEvent) {
		if (!hasRecentlyMerged && (System.currentTimeMillis() - lastMergeTime) > 60_000L) {
			lastMergeTime = System.currentTimeMillis()
			doMerge()
		}
	}

	fun logEntry(entry: LedgerEntry) {
		if (shouldLog)
			printToChat(entry)
		Ledger.logger.info("Logging entry of type ${entry.transactionType}")
		val transactionId = UUIDUtil.createULIDAt(entry.timestamp)
		DBLogEntry.insert(database.connection) {
			it[DBLogEntry.profileId] = currentProfile ?: UUIDUtil.NIL_UUID
			it[DBLogEntry.playerId] = UUIDUtil.getPlayerUUID()
			it[DBLogEntry.type] = entry.transactionType
			it[DBLogEntry.transactionId] = transactionId
		}
		entry.items.forEach { change ->
			DBItemEntry.insert(database.connection) {
				it[DBItemEntry.transactionId] = transactionId
				it[DBItemEntry.mode] = change.direction
				it[DBItemEntry.size] = change.count
				it[DBItemEntry.itemId] = change.itemId
			}
		}
		entries.add(entry.intoJson(currentProfile))
		commit()
	}

	fun commit() {
		try {
			hasRecentlyMerged = false
			file.writeText(gson.toJson(entries))
		} catch (ex: Exception) {
			Ledger.logger.error("Could not save file", ex)
		}
	}

	val gson = Gson()

	val folder = Ledger.dataFolder
	val file: File = run {
		val date = SimpleDateFormat("yyyy.MM.dd").format(Date())

		generateSequence(0) { it + 1 }
			.map {
				if (it == 0)
					folder.resolve("$date.json")
				else
					folder.resolve("$date-$it.json")
			}
			.filter { !it.exists() }
			.first()
	}
}

enum class TransactionType {
	AUCTION_BOUGHT,
	AUCTION_SOLD,
	AUTOMERCHANT_PROFIT_COLLECT,
	BANK_DEPOSIT,
	BANK_WITHDRAW,
	BAZAAR_BUY_INSTANT,
	BAZAAR_BUY_ORDER,
	BAZAAR_SELL_INSTANT,
	BAZAAR_SELL_ORDER,
	BITS_PURSE_STATUS,
	BOOSTER_COOKIE_ATE,
	COMMUNITY_SHOP_BUY,
	DUNGEON_CHEST_OPEN,
	KAT_TIMESKIP,
	KAT_UPGRADE,
	KISMET_REROLL,
	NPC_BUY,
	NPC_SELL,
}

@JvmInline
value class ItemId(
	val string: String
) {
	companion object {
		val COINS = ItemId("SKYBLOCK_COIN")
		val BITS = ItemId("SKYBLOCK_BIT")
		val NIL = ItemId("SKYBLOCK_NIL")
		val DUNGEON_CHEST_KEY = ItemId("DUNGEON_CHEST_KEY")
		val BOOSTER_COOKIE = ItemId("BOOSTER_COOKIE")
		val KISMET_FEATHER = ItemId("KISMET_FEATHER")
	}
}


data class ItemChange(
	val itemId: ItemId,
	val count: Double,
	val direction: ChangeDirection,
) {
	enum class ChangeDirection {
		GAINED,
		TRANSFORM,
		SYNC,
		CATALYST,
		LOST;
	}

	companion object {
		fun gainCoins(number: Double): ItemChange {
			return gain(ItemId.COINS, number)
		}

		fun gain(itemId: ItemId, amount: Number): ItemChange {
			return ItemChange(itemId, amount.toDouble(), ChangeDirection.GAINED)
		}

		fun lose(itemId: ItemId, amount: Number): ItemChange {
			return ItemChange(itemId, amount.toDouble(), ChangeDirection.LOST)
		}

		fun loseCoins(number: Double): ItemChange {
			return lose(ItemId.COINS, number)
		}
	}
}

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

