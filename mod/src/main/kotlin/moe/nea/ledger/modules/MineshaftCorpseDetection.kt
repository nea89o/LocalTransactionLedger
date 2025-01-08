package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.ItemIdProvider
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.matches
import moe.nea.ledger.useMatcher
import moe.nea.ledger.utils.BorderedTextTracker
import moe.nea.ledger.utils.di.Inject

class MineshaftCorpseDetection : BorderedTextTracker() {
	/*
[23:39:47] [Client thread/INFO]: [CHAT] §r§a§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬§r
[23:39:47] [Client thread/INFO]: [CHAT] §r  §r§b§l§r§9§lLAPIS §r§b§lCORPSE LOOT! §r
[23:39:47] [Client thread/INFO]: [CHAT] §r  §r§a§lREWARDS§r
[23:39:47] [Client thread/INFO]: [CHAT] §r    §r§5+100 HOTM Experience§r
[23:39:47] [Client thread/INFO]: [CHAT] §r    §r§a§r§aGreen Goblin Egg§r
[23:39:47] [Client thread/INFO]: [CHAT] §r    §r§9Enchanted Glacite §r§8x2§r
[23:39:47] [Client thread/INFO]: [CHAT] §r    §r§9☠ Fine Onyx Gemstone§r
[23:39:47] [Client thread/INFO]: [CHAT] §r    §r§a☠ Flawed Onyx Gemstone §r§8x20§r
[23:39:47] [Client thread/INFO]: [CHAT] §r    §r§a☘ Flawed Peridot Gemstone §r§8x40§r
[23:39:47] [Client thread/INFO]: [CHAT] §r    §r§bGlacite Powder §r§8x500§r
[23:39:47] [Client thread/INFO]: [CHAT] §e[SkyHanni] Profit for §9Lapis Corpse§e: §678k§r
[23:39:47] [Client thread/INFO]: [CHAT] §r§a§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬§r
	*/

	val corpseEnterMessage = "  (?<corpseKind>.*) CORPSE LOOT!".toPattern()

	override fun shouldEnter(event: ChatReceived): Boolean {
		return corpseEnterMessage.matches(event.message)
	}

	override fun shouldExit(event: ChatReceived): Boolean {
		return genericBorderExit.matches(event.message)
	}

	override fun onBorderedTextFinished(enclosed: List<ChatReceived>) {
		val rewards = enclosed.asSequence()
			.dropWhile { it.message != "  REWARDS" }
			.drop(1)
			.mapNotNull {
				itemIdProvider.findStackableItemByName(it.message, true)
			}
			.map { ItemChange.unpairGain(it) }
			.toMutableList()
		val introMessage = enclosed.first()
		val corpseTyp = corpseEnterMessage.useMatcher(introMessage.message) {
			group("corpseKind")
		}!!
		val keyTyp = corpseNameToKey[corpseTyp]
		if (keyTyp == null) {
			errorUtil.reportAdHoc("Unknown corpse type $corpseTyp")
		} else if (keyTyp != ItemId.NIL) {
			rewards.add(ItemChange.lose(keyTyp, 1))
		}
		logger.logEntry(
			LedgerEntry(
				TransactionType.CORPSE_DESECRATED,
				introMessage.timestamp,
				rewards
			)
		)
	}

	val corpseNameToKey = mapOf(
		"LAPIS" to ItemId.NIL,
		"VANGUARD" to ItemId("SKELETON_KEY"),
		"UMBER" to ItemId("UMBER_KEY"),
		"TUNGSTEN" to ItemId("TUNGSTEN_KEY"),
	)

	@Inject
	lateinit var logger: LedgerLogger

	@Inject
	lateinit var itemIdProvider: ItemIdProvider
}