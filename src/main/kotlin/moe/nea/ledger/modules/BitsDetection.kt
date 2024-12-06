package moe.nea.ledger.modules

import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.events.LateWorldLoadEvent
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.SHORT_NUMBER_PATTERN
import moe.nea.ledger.ScoreboardUtil
import moe.nea.ledger.parseShortNumber
import moe.nea.ledger.unformattedString
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.Inject
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
                            "BITS_PURSE_STATUS",
                            Instant.now(),
                            0.0,
                            null,
                            bits
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
                    "BOOSTER_COOKIE_ATE",
                    Instant.now(),
                    0.0,
                    null,
                    null,
                )
            )
        }
    }
}
