package moe.nea.ledger

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.ChatComponentText
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class LedgerLogger {
    fun printOut(text: String) {
        Minecraft.getMinecraft().ingameGUI?.chatGUI?.printChatMessage(ChatComponentText(text))
    }

    val connection = DriverManager.getConnection("jdbc:sqlite:money-ledger/database.db")

    val profileIdPattern =
        "Profile ID: (?<profile>[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})".toPattern()

    var currentProfile: String? = null

    var shouldLog = false

    init {
        ClientCommandHandler.instance.registerCommand(object : CommandBase() {
            override fun getCommandName(): String {
                return "ledgerlogchat"
            }

            override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
                return true
            }

            override fun getCommandUsage(sender: ICommandSender?): String {
                return ""
            }

            override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
                shouldLog = !shouldLog
                printOut("§eLedger logging toggled " + (if (shouldLog) "§aon" else "§coff") + "§e.")
            }
        })
    }

    @SubscribeEvent
    fun onProfileSwitch(event: ChatReceived) {
        profileIdPattern.useMatcher(event.message) {
            currentProfile = group("profile")
        }
    }


    fun printToChat(entry: LedgerEntry) {
        printOut(
            """
            §e================= TRANSACTION START
            §eTYPE: §a${entry.transactionType}
            §eTIMESTAMP: §a${entry.timestamp}
            §eTOTAL VALUE: §a${entry.totalTransactionCoins}
            §eITEM ID: §a${entry.itemId}
            §eITEM AMOUNT: §a${entry.itemAmount}
            §ePROFILE: §a${currentProfile}
            §e================= TRANSACTION END
            """.trimIndent()
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

    val folder = File("money-ledger").also { it.mkdirs() }
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


data class LedgerEntry(
    val transactionType: String,
    val timestamp: Instant,
    val totalTransactionCoins: Double,
    val itemId: String? = null,
    val itemAmount: Int? = null,
) {
    fun intoJson(profileId: String?): JsonObject {
        return JsonObject().apply {
            addProperty("transactionType", transactionType)
            addProperty("timestamp", timestamp.toEpochMilli().toString())
            addProperty("totalTransactionValue", totalTransactionCoins)
            addProperty("itemId", itemId ?: "")
            addProperty("itemAmount", itemAmount ?: 0)
            addProperty("profileId", profileId)
            addProperty(
                "playerId",
                (Minecraft.getMinecraft().thePlayer?.uniqueID?.toString() ?: lastKnownUUID)
                    .also { lastKnownUUID = it })
        }
    }
}

var lastKnownUUID = "null"
