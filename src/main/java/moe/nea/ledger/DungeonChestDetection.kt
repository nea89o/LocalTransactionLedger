package moe.nea.ledger

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.time.Instant
import java.util.regex.Pattern

class DungeonChestDetection(val logger: LedgerLogger) {

    /*{
        id: "minecraft:chest",
        Count: 1b,
        tag: {
            display: {
                Lore: ["§7Purchase this chest to receive the", "§7rewards above. You can only open", "§7one chest per Dungeons run -", "§7choose wisely!", "", "§7Cost", "§625,000 Coins", "§9Dungeon Chest Key", "", "§7§cNOTE: Coins are withdrawn from your", "§cbank if you don't have enough in", "§cyour purse."],
                Name: "§aOpen Reward Chest"
            }
        },
        Damage: 0s
    }*/
    val costPattern = Pattern.compile("(?<cost>$SHORT_NUMBER_PATTERN) Coins")


    data class ChestCost(
        val cost: Double,
        val openTimestamp: Long,
        val hasKey: Boolean,
    )

    var lastOpenedChest: ChestCost? = null

    @SubscribeEvent
    fun onSlotClick(event: GuiClickEvent) {
        val slot = event.slotIn ?: return
        if (!slot.inventory.displayName.unformattedText.unformattedString().endsWith(" Chest")) return
        val stack = slot.stack ?: return
        val name = stack.getDisplayNameU()
        if (name != "§aOpen Reward Chest") return
        val lore = stack.getLore()
        val costIndex = lore.indexOf("§7Cost")
        if (costIndex < 0 || costIndex + 1 !in lore.indices) return
        val cost = costPattern.useMatcher(lore[costIndex + 1].unformattedString()) {
            parseShortNumber(group("cost"))
        } ?: 0.0 // Free chest!
        val hasKey = lore.contains("§9Dungeon Chest Key")
        lastOpenedChest?.let(::completeTransaction)
        lastOpenedChest = ChestCost(cost, System.currentTimeMillis(), hasKey)
    }

    @SubscribeEvent
    fun onChatMessage(event: ChatReceived) {
        if (event.message == "You don't have that many coins in the bank!")
            lastOpenedChest = null
    }

    fun completeTransaction(toOpen: ChestCost) {
        lastOpenedChest = null
        logger.logEntry(
            LedgerEntry(
                "DUNGEON_CHEST_OPEN",
                Instant.ofEpochMilli(toOpen.openTimestamp),
                toOpen.cost,
                itemId = if (toOpen.hasKey) "DUNGEON_CHEST_KEY" else null
            )
        )
    }

    @SubscribeEvent
    fun onTick(event: TickEvent) {
        val toOpen = lastOpenedChest
        if (toOpen != null && toOpen.openTimestamp + 1000L < System.currentTimeMillis()) {
            completeTransaction(toOpen)
        }
    }
}
