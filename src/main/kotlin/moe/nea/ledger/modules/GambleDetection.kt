package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.di.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GambleDetection {

	val dieRolled =
		"Your (?<isHighClass>High Class )?Archfiend Dice rolled a (?<face>[1-7])!.*"
			.toPattern()

	@Inject
	lateinit var logger: LedgerLogger

	@SubscribeEvent
	fun onChat(event: ChatReceived) {
		dieRolled.useMatcher(event.message) {
			val isLowClass = group("isHighClass").isNullOrBlank()
			val item = if (isLowClass) ItemId.ARCHFIEND_LOW_CLASS else ItemId.ARCHFIEND_HIGH_CLASS
			val face = group("face")
			val rollCost = if (isLowClass) 666_000.0 else 6_600_000.0
			if (face == "7") {
				logger.logEntry(LedgerEntry(
					TransactionType.DIE_ROLLED,
					event.timestamp,
					listOf(
						ItemChange.lose(item, 1),
						ItemChange.loseCoins(rollCost),
						ItemChange.gain(ItemId.ARCHFIEND_DYE, 1),
					)
				))
			} else if (face == "6") {
				logger.logEntry(LedgerEntry(
					TransactionType.DIE_ROLLED,
					event.timestamp,
					listOf(
						ItemChange.lose(item, 1),
						ItemChange.loseCoins(rollCost),
						ItemChange.gainCoins(if (isLowClass) 15_000_000.0 else 100_000_000.0),
					)
				))
			} else {
				logger.logEntry(LedgerEntry(
					TransactionType.DIE_ROLLED,
					event.timestamp,
					listOf(
						ItemChange(item, 1.0, ItemChange.ChangeDirection.CATALYST),
						ItemChange.loseCoins(rollCost),
					)
				))
			}
		}
	}
}