package moe.nea.ledger

import com.google.gson.Gson
import io.github.notenoughupdates.moulconfig.managed.ManagedConfig
import moe.nea.ledger.config.LedgerConfig
import moe.nea.ledger.config.UpdateUi
import moe.nea.ledger.config.UpdateUiMarker
import moe.nea.ledger.database.Database
import moe.nea.ledger.events.ChatReceived
import moe.nea.ledger.events.LateWorldLoadEvent
import moe.nea.ledger.events.RegistrationFinishedEvent
import moe.nea.ledger.events.WorldSwitchEvent
import moe.nea.ledger.gen.BuildConfig
import moe.nea.ledger.modules.AuctionHouseDetection
import moe.nea.ledger.modules.BankDetection
import moe.nea.ledger.modules.BazaarDetection
import moe.nea.ledger.modules.BazaarOrderDetection
import moe.nea.ledger.modules.BitsDetection
import moe.nea.ledger.modules.BitsShopDetection
import moe.nea.ledger.modules.DragonEyePlacementDetection
import moe.nea.ledger.modules.`DragonSacrificeDetection`
import moe.nea.ledger.modules.DungeonChestDetection
import moe.nea.ledger.modules.ExternalDataProvider
import moe.nea.ledger.modules.EyedropsDetection
import moe.nea.ledger.modules.ForgeDetection
import moe.nea.ledger.modules.GambleDetection
import moe.nea.ledger.modules.GodPotionDetection
import moe.nea.ledger.modules.GodPotionMixinDetection
import moe.nea.ledger.modules.KatDetection
import moe.nea.ledger.modules.KuudraChestDetection
import moe.nea.ledger.modules.MineshaftCorpseDetection
import moe.nea.ledger.modules.MinionDetection
import moe.nea.ledger.modules.NpcDetection
import moe.nea.ledger.modules.UpdateChecker
import moe.nea.ledger.modules.VisitorDetection
import moe.nea.ledger.utils.ErrorUtil
import moe.nea.ledger.utils.MinecraftExecutor
import moe.nea.ledger.utils.di.DI
import moe.nea.ledger.utils.di.DIProvider
import moe.nea.ledger.utils.network.RequestUtil
import net.minecraft.client.Minecraft
import net.minecraft.command.ICommand
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

@Mod(modid = "ledger", useMetadata = true, version = BuildConfig.VERSION)
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
	// You bought back Potato x3 for 9 Coins!

	TODO: TRADING, FORGE, VISITORS / COPPER, CORPSES ÖFFNEN, HIGH / LOW GAMBLES, MINION ITEMS (maybe inferno refuel)
	TODO: PET LEVELING COSTS AT FANN, SLAYER / MOB DROPS, SLAYER START COST
	*/
	companion object {
		val dataFolder = File("money-ledger").apply { mkdirs() }
		val logger = LogManager.getLogger("MoneyLedger")
		val managedConfig = ManagedConfig.create(File("config/money-ledger/config.json"), LedgerConfig::class.java) {
			checkExpose = false
			customProcessor<UpdateUiMarker> { option, ann ->
				UpdateUi(option)
			}
		}
		val gson = Gson()
		private val tickQueue = ConcurrentLinkedQueue<Runnable>()
		fun runLater(runnable: Runnable) {
			tickQueue.add(runnable)
		}

		val di = DI()
	}

	@Mod.EventHandler
	fun init(event: FMLInitializationEvent) {
		logger.info("Initializing ledger")

		TelemetryProvider.setupFor(di)
		di.registerSingleton(this)
		di.registerSingleton(Minecraft.getMinecraft())
		di.registerSingleton(gson)
		di.register(LedgerConfig::class.java, DIProvider { managedConfig.instance })
		di.registerInjectableClasses(
			AuctionHouseDetection::class.java,
			BankDetection::class.java,
			BazaarDetection::class.java,
			BazaarOrderDetection::class.java,
			BitsDetection::class.java,
			BitsShopDetection::class.java,
			ConfigCommand::class.java,
			Database::class.java,
			DebugDataCommand::class.java,
			DragonEyePlacementDetection::class.java,
			DragonSacrificeDetection::class.java,
			DungeonChestDetection::class.java,
			ErrorUtil::class.java,
			ExternalDataProvider::class.java,
			EyedropsDetection::class.java,
			ForgeDetection::class.java,
			GambleDetection::class.java,
			GodPotionDetection::class.java,
			GodPotionMixinDetection::class.java,
			ItemIdProvider::class.java,
			KatDetection::class.java,
			KuudraChestDetection::class.java,
			LedgerLogger::class.java,
			LogChatCommand::class.java,
			MinecraftExecutor::class.java,
			MineshaftCorpseDetection::class.java,
			MinionDetection::class.java,
			NpcDetection::class.java,
			QueryCommand::class.java,
			RequestUtil::class.java,
			TriggerCommand::class.java,
			UpdateChecker::class.java,
			VisitorDetection::class.java,
		)
		val errorUtil = di.provide<ErrorUtil>()
		errorUtil.catch {
			di.instantiateAll()
			di.getAllInstances().forEach(MinecraftForge.EVENT_BUS::register)
			di.getAllInstances().filterIsInstance<ICommand>()
				.forEach { ClientCommandHandler.instance.registerCommand(it) }
		}

		errorUtil.catch {
			di.provide<Database>().loadAndUpgrade()
		}

		MinecraftForge.EVENT_BUS.post(RegistrationFinishedEvent())
	}

	var lastJoin = -1L

	@SubscribeEvent
	fun worldSwitchEvent(event: EntityJoinWorldEvent) {
		if (event.entity == Minecraft.getMinecraft().thePlayer) {
			lastJoin = System.currentTimeMillis()
			MinecraftForge.EVENT_BUS.post(WorldSwitchEvent())
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
