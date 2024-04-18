package moe.nea.ledger

import net.minecraft.client.Minecraft
import net.minecraft.command.CommandBase
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.apache.logging.log4j.LogManager

@Mod(modid = "ledger", useMetadata = true)
class Ledger {
    /*
    You have withdrawn 1M coins! You now have 518M coins in your account!
    You have deposited 519M coins! You now have 519M coins in your account!

    // ORDERS:

    [Bazaar] Buy Order Setup! 160x Wheat for 720.0 coins.
    [Bazaar] Claimed 160x Wheat worth 720.0 coins bought for 4.5 each!

    [Bazaar] Sell Offer Setup! 160x Wheat for 933.4 coins.
    [Bazaar] Claimed 34,236,799 coins from selling 176x Hyper Catalyst at 196,741 each!

    // INSTABUY:

    [Bazaar] Bought 64x Wheat for 377.6 coins!
    [Bazaar] Sold 64x Wheat for 268.8 coins!

    // AUCTION HOUSE:

    You collected 8,712,000 coins from selling Ultimate Carrot Candy Upgrade to [VIP] kodokush in an auction!
    You purchased 2x Walnut for 69 coins!
    You purchased ◆ Ice Rune I for 4,000 coins!

    TODO: TRADING, FORGE, COOKIE_EATEN, NPC_SELL, NPC_BUY
    */
    companion object {
        val logger = LogManager.getLogger("MoneyLedger")
    }

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        logger.info("Initializing ledger")
        val ledger = LedgerLogger()
        val ids = ItemIdProvider()
        listOf(
            this,
            ids,
            ledger,
            BankDetection(ledger),
            BazaarDetection(ledger, ids),
            DungeonChestDetection(ledger),
            BazaarOrderDetection(ledger, ids),
            AuctionHouseDetection(ledger, ids),
            BitsDetection(ledger),
            BitsShop(ledger),
        ).forEach(MinecraftForge.EVENT_BUS::register)
    }

    var lastJoin = -1L

    @SubscribeEvent
    fun worldSwitchEvent(event: EntityJoinWorldEvent) {
        if (event.entity == Minecraft.getMinecraft().thePlayer) {
            lastJoin = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun tickEvent(event: ClientTickEvent) {
        if (event.phase == TickEvent.Phase.END
            && lastJoin > 0
            && System.currentTimeMillis() - lastJoin > 10_000
            && Minecraft.getMinecraft().thePlayer != null
        ) {
            lastJoin = -1
            MinecraftForge.EVENT_BUS.post(LateWorldLoadEvent())
        }
    }

    @SubscribeEvent(receiveCanceled = true, priority = EventPriority.HIGHEST)
    fun onChat(event: ClientChatReceivedEvent) {
        if (event.type != 2.toByte())
            MinecraftForge.EVENT_BUS.post(ChatReceived(event))
    }
}
