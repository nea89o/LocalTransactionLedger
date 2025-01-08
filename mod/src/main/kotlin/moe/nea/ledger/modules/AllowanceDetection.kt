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

class AllowanceDetection {

	val allowancePattern =
		Pattern.compile("ALLOWANCE! You earned (?<coins>$SHORT_NUMBER_PATTERN) coins!")

	@Inject
	lateinit var logger: LedgerLogger

	@SubscribeEvent
	fun onAllowanceGain(event: ChatReceived) {
		allowancePattern.useMatcher(event.message) {
			logger.logEntry(
				LedgerEntry(
					TransactionType.ALLOWANCE_GAIN,
					event.timestamp,
					listOf(
						ItemChange.gainCoins(parseShortNumber(group("coins"))),
					)
				)
			)
		}
	}
}
