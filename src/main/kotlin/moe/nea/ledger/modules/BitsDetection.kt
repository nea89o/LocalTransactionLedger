package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.events.LateWorldLoadEvent
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.SHORT_NUMBER_PATTERN
import moe.nea.ledger.ScoreboardUtil
import moe.nea.ledger.TransactionType
import moe.nea.ledger.parseShortNumber
import moe.nea.ledger.unformattedString
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.di.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.time.Instant

class BitsDetection @Inject constructor(val ledger: LedgerLogger) {

    var lastBits = -1

    val bitScoreboardRegex = "Bits: (?<purse>$SHORT_NUMBER_PATTERN)".toPattern()

    @SubscribeEvent
    fun onWorldSwitch(event: LateWorldLoadEvent) {
        ScoreboardUtil.getScoreboardStrings().forEach {
            bitScoreboardRegex.useMatcher<Unit>(it.unformattedString()) {
                val bits = parseShortNumber(group("purse")).toInt()
                if (lastBits != bits) {
                    ledger.logEntry(
                        LedgerEntry(
	                        TransactionType.BITS_PURSE_STATUS,
	                        Instant.now(),
	                        listOf(
								ItemChange(ItemId.BITS, bits.toDouble(), ItemChange.ChangeDirection.SYNC)
							)
                        )
                    )
                    lastBits = bits
                }
                return
            }
        }
    }

    @SubscribeEvent
    fun onEvent(event: ChatReceived) {
        if (event.message.startsWith("You consumed a Booster Cookie!")) {
            ledger.logEntry(
                LedgerEntry(
	                TransactionType.BOOSTER_COOKIE_ATE,
                    Instant.now(),
					listOf(
						ItemChange.lose(ItemId.BOOSTER_COOKIE, 1)
					)
                )
            )
        }
    }
}
