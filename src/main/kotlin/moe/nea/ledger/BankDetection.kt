package moe.nea.ledger

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class BankDetection(val ledger: LedgerLogger) {

    /*
        You have withdrawn 1M coins! You now have 518M coins in your account!
        You have deposited 519M coins! You now have 519M coins in your account!
     */


    val withdrawPattern =
        Pattern.compile("^You have withdrawn (?<amount>$SHORT_NUMBER_PATTERN) coins?! You now have (?<newtotal>$SHORT_NUMBER_PATTERN) coins? in your account!$")
    val depositPattern =
        Pattern.compile("^You have deposited (?<amount>$SHORT_NUMBER_PATTERN) coins?! You now have (?<newtotal>$SHORT_NUMBER_PATTERN) coins? in your account!$")
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
