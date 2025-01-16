package moe.nea.ledger.modules

import moe.nea.ledger.ItemChange
import moe.nea.ledger.ItemId
import moe.nea.ledger.LedgerEntry
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.TransactionType
import moe.nea.ledger.events.GuiClickEvent
import moe.nea.ledger.getInternalId
import moe.nea.ledger.utils.di.Inject
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class KuudraChestDetection : ChestDetection() {
	// TODO: extra essence for kuudra pet (how?), item SALVAGE detection
	// TODO: save uuid along side item id

	val kuudraKeyPattern = "KUUDRA_.*_TIER_KEY".toPattern()

	@Inject
	lateinit var log: LedgerLogger

	@Inject
	lateinit var minecraft: Minecraft
	fun hasKey(keyItem: ItemId): Boolean {
		val p = minecraft.thePlayer ?: return false
		return p.inventory.mainInventory.any { it?.getInternalId() == keyItem }
	}

	@SubscribeEvent
	fun onRewardChestClick(event: GuiClickEvent) {
		val diffs = scrapeChestReward(event.slotIn ?: return) ?: return
		val requiredKey = diffs.diff.find {
			it.direction == ItemChange.ChangeDirection.LOST && kuudraKeyPattern.asPredicate().test(it.itemId.string)
		}?.itemId
		if (requiredKey != null && !hasKey(requiredKey)) {
			return
		}
		if (requiredKey == null && event.slotIn.inventory.name != "Free Chest") {
			return
		}
		log.logEntry(LedgerEntry(
			TransactionType.KUUDRA_CHEST_OPEN,
			diffs.timestamp,
			diffs.diff,
		))
	}
}