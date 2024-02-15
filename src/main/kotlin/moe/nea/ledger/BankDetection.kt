package moe.nea.ledger

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class BankDetection(val ledger: LedgerLogger) {
    val withdrawPattern =
        Pattern.compile("^(You have withdrawn|Withdrew) (?<amount>$SHORT_NUMBER_PATTERN) coins?! (?:There's now|You now have) (?<newtotal>$SHORT_NUMBER_PATTERN) coins? (?:left in the account!|in your account!)$")
    val depositPattern =
        Pattern.compile("^(?:You have deposited|Deposited) (?<amount>$SHORT_NUMBER_PATTERN) coins?! (?:There's now|You now have) (?<newtotal>$SHORT_NUMBER_PATTERN) coins? (?:in your account!|in the account!)$")
    @SubscribeEvent
    fun onChat(event: ChatReceived) {
        withdrawPattern.useMatcher(event.message) {
            ledger.logEntry(
                LedgerEntry(
                    "BANK_WITHDRAW",
                    event.timestamp,
                    parseShortNumber(group("amount")),
                )
            )
        }
        depositPattern.useMatcher(event.message) {
            ledger.logEntry(
                LedgerEntry(
                    "BANK_DEPOSIT",
                    event.timestamp,
                    parseShortNumber(group("amount")),
                )
            )
        }
    }

}
