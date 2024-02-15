package moe.nea.ledger

import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import java.time.Instant

class LedgerLogger {
    fun printOut(text: String) {
        Minecraft.getMinecraft().ingameGUI?.chatGUI?.printChatMessage(ChatComponentText(text))
    }

    fun logEntry(entry: LedgerEntry) {
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

}

data class LedgerEntry(
    val transactionType: String,
    val timestamp: Instant,
    val totalTransactionCoins: Double,
    val itemId: String? = null,
    val itemAmount: Int? = null,
)
