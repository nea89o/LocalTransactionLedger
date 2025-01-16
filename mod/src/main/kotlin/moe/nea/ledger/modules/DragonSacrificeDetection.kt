package moe.nea.ledger.modules

import moe.nea.ledger.DebouncedValue
import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemIdProvider
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.SHORT_NUMBER_PATTERN
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.gen.ItemIds
import moe.nea.ledger.parseShortNumber
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.di.Inject
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.time.Duration.Companion.seconds

class DragonSacrificeDetection {
	//SACRIFICE! You turned Holy Dragon Boots into 30 Dragon Essence!
	//BONUS LOOT! You also received 17x Holy Dragon Fragment from your sacrifice!
	@Inject
	lateinit var itemIdProvider: ItemIdProvider

	@Inject
	lateinit var logger: LedgerLogger

	val sacrificePattern =
		"SACRIFICE! You turned (?<sacrifice>.*) into (?<amount>$SHORT_NUMBER_PATTERN) Dragon Essence!".toPattern()
	val bonusLootPattern = "BONUS LOOT! You also received (?<bonus>.*) from your sacrifice!".toPattern()

	var lastSacrifice: DebouncedValue<LedgerEntry> = DebouncedValue.farFuture()


	@SubscribeEvent
	fun onChat(event: ChatReceived) {
		sacrificePattern.useMatcher(event.message) {
			val sacrifice = itemIdProvider.findForName(group("sacrifice")) ?: return
			val lootEssence = parseShortNumber(group("amount"))
			consume(lastSacrifice.replace())
			lastSacrifice = DebouncedValue(LedgerEntry(
				TransactionType.DRACONIC_SACRIFICE,
				event.timestamp,
				listOf(
					ItemChange.lose(sacrifice, 1),
					ItemChange.gain(ItemIds.ESSENCE_DRAGON, lootEssence)
				)
			))
		}
		bonusLootPattern.useMatcher(event.message) {
			val bonusItem = itemIdProvider.findStackableItemByName(
				group("bonus"), true
			) ?: return
			lastSacrifice.replace()?.let {
				consume(
					it.copy(items = it.items + ItemChange.unpairGain(bonusItem))
				)
			}
		}
	}

	@SubscribeEvent
	fun onTick(event: TickEvent) {
		consume(lastSacrifice.consume(4.seconds))
	}

	fun consume(entry: LedgerEntry?) {
		if (entry != null)
			logger.logEntry(entry)
	}
}