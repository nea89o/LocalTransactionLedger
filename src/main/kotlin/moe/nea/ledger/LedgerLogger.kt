package moe.nea.ledger

import com.google.gson.Gson
import com.google.gson.JsonArray
import moe.nea.ledger.database.DBItemEntry
import moe.nea.ledger.database.DBLogEntry
import moe.nea.ledger.database.Database
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.utils.Inject
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

class LedgerLogger {
	fun printOut(text: String) = printOut(ChatComponentText(text))
	fun printOut(comp: IChatComponent) {
		Minecraft.getMinecraft().ingameGUI?.chatGUI?.printChatMessage(comp)
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


