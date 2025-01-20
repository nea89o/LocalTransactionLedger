package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.SHORT_NUMBER_PATTERN
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.parseShortNumber
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.di.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern


class BankInterestDetection {

    val bankInterestPattern =
        Pattern.compile("You have just received (?<coins>$SHORT_NUMBER_PATTERN) coins as interest in your (co-op|personal) bank account!")
    val offlineBankInterestPattern =
        Pattern.compile("Since you've been away you earned (?<coins>$SHORT_NUMBER_PATTERN) coins as interest in your personal bank account!")

    @Inject
    lateinit var logger: LedgerLogger

    @SubscribeEvent
    fun onChat(event: ChatReceived) {
        bankInterestPattern.useMatcher(event.message) {
            logger.logEntry(
                LedgerEntry(
                    TransactionType.BANK_WITHDRAW,
                    event.timestamp,
                    listOf(
                        ItemChange.gainCoins(parseShortNumber(group("coins"))),
                    )
                )
            )
        }
        offlineBankInterestPattern.useMatcher(event.message) {
            logger.logEntry(
                LedgerEntry(
                    TransactionType.BANK_WITHDRAW,
                    event.timestamp,
                    listOf(
                        ItemChange.gainCoins(parseShortNumber(group("coins"))),
                    )
                )
            )
        }
    }
}
