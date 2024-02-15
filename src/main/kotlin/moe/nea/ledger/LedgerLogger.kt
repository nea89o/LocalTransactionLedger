package moe.nea.ledger

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

class LedgerLogger {
    fun printOut(text: String) {
        Minecraft.getMinecraft().ingameGUI?.chatGUI?.printChatMessage(ChatComponentText(text))
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
            §e================= TRANSACTION END
            """.trimIndent()
        )
    }

    val entries = JsonArray()

    fun logEntry(entry: LedgerEntry) {
        Ledger.logger.info("Logging entry of type ${entry.transactionType}")
        entries.add(entry.intoJson())
        commit()
    }

    fun commit() {
        try {
            file.writeText(gson.toJson(entries))
        } catch (ex: Exception) {
            Ledger.logger.error("Could not save file", ex)
        }
    }

    val gson = Gson()

    val file: File = run {
        val folder = File("money-ledger")
        folder.mkdirs()
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
    fun intoJson(): JsonObject {
        return JsonObject().apply {
            addProperty("transactionType", transactionType)
            addProperty("timestamp", timestamp.toEpochMilli().toString())
            addProperty("totalTransactionValue", totalTransactionCoins)
            addProperty("itemId", itemId ?: "")
            addProperty("itemAmount", itemAmount ?: 0)
        }
    }
}
