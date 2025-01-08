package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
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


class BankDetection @Inject constructor(val ledger: LedgerLogger) {
	val withdrawPattern =
		Pattern.compile("^(You have withdrawn|Withdrew) (?<amount>$SHORT_NUMBER_PATTERN) coins?! (?:There's now|You now have) (?<newtotal>$SHORT_NUMBER_PATTERN) coins? (?:left in the account!|in your account!)$")
	val depositPattern =
		Pattern.compile("^(?:You have deposited|Deposited) (?<amount>$SHORT_NUMBER_PATTERN) coins?! (?:There's now|You now have) (?<newtotal>$SHORT_NUMBER_PATTERN) coins? (?:in your account!|in the account!)$")

	@SubscribeEvent
	fun onChat(event: ChatReceived) {
		withdrawPattern.useMatcher(event.message) {
			ledger.logEntry(
				LedgerEntry(
					TransactionType.BANK_WITHDRAW,
					event.timestamp,
					listOf(ItemChange(ItemId.COINS,
					                  parseShortNumber(group("amount")),
					                  ItemChange.ChangeDirection.TRANSFORM)),
				)
			)
		}
		depositPattern.useMatcher(event.message) {
			ledger.logEntry(
				LedgerEntry(
					TransactionType.BANK_DEPOSIT,
					event.timestamp,
					listOf(ItemChange(ItemId.COINS,
					                  parseShortNumber(group("amount")),
					                  ItemChange.ChangeDirection.TRANSFORM)),
				)
			)
		}
	}

}
