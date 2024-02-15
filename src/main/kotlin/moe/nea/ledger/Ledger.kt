package moe.nea.ledger

import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

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

    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        val ledger = LedgerLogger()
        val ids = ItemIdProvider()
        listOf(
            this,
            ids,
            BankDetection(ledger),
            BazaarDetection(ledger, ids),
            AuctionHouseDetection(ledger, ids),
        ).forEach(MinecraftForge.EVENT_BUS::register)
    }

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (event.type != 2.toByte())
            MinecraftForge.EVENT_BUS.post(ChatReceived(event))
    }
}
