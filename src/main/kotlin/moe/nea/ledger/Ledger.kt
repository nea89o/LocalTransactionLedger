package moe.nea.ledger

import io.github.notenoughupdates.moulconfig.common.IMinecraft
import io.github.notenoughupdates.moulconfig.managed.ManagedConfig
import moe.nea.ledger.config.LedgerConfig
import moe.nea.ledger.database.Database
import net.minecraft.client.Minecraft
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
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
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

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

	// NPC

	// You bought Cactus x32 for 465.6 Coins!
    // You sold Cactus x1 for 3 Coins!

	TODO: TRADING, FORGE, COOKIE_EATEN
	*/
	companion object {
		val dataFolder = File("money-ledger").apply { mkdirs() }
		val logger = LogManager.getLogger("MoneyLedger")
		val managedConfig = ManagedConfig.create(File("config/money-ledger/config.json"), LedgerConfig::class.java) {
			checkExpose = false
		}
		private val tickQueue = ConcurrentLinkedQueue<Runnable>()
		fun runLater(runnable: Runnable) {
			tickQueue.add(runnable)
		}
	}

	@Mod.EventHandler
	fun init(event: FMLInitializationEvent) {
		logger.info("Initializing ledger")
		Database.init()

		ClientCommandHandler.instance.registerCommand(object : CommandBase() {
			override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
				return true
			}

			override fun getCommandName(): String {
				return "ledger"
			}

			override fun getCommandUsage(sender: ICommandSender?): String {
				return ""
			}

			override fun processCommand(sender: ICommandSender?, args: Array<out String>) {
				val editor = managedConfig.getEditor()
				editor.search(args.joinToString(" "))
				runLater {
					IMinecraft.instance.openWrappedScreen(editor)
				}
			}

			override fun getCommandAliases(): List<String> {
				return listOf("moneyledger")
			}
		})
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
			MinionDetection(ledger),
			NpcDetection(ledger, ids),
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
		while (true) {
			val queued = tickQueue.poll() ?: break
			queued.run()
		}
	}

	@SubscribeEvent(receiveCanceled = true, priority = EventPriority.HIGHEST)
	fun onChat(event: ClientChatReceivedEvent) {
		if (event.type != 2.toByte())
			MinecraftForge.EVENT_BUS.post(ChatReceived(event))
	}
}
