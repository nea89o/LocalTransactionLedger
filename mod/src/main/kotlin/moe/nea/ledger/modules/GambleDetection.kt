package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.gen.ItemIds
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
			val item = if (isLowClass) ItemIds.ARCHFIEND_DICE else ItemIds.HIGH_CLASS_ARCHFIEND_DICE
			val face = group("face")
			val rollCost = if (isLowClass) 666_000.0 else 6_600_000.0
			if (face == "7") {
				logger.logEntry(LedgerEntry(
					TransactionType.DIE_ROLLED,
					event.timestamp,
					listOf(
						ItemChange.lose(item, 1),
						ItemChange.loseCoins(rollCost),
						ItemChange.gain(ItemIds.DYE_ARCHFIEND, 1),
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