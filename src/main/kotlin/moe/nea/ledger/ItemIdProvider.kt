package moe.nea.ledger

import moe.nea.ledger.events.BeforeGuiAction
import moe.nea.ledger.events.ExtraSupplyIdEvent
import moe.nea.ledger.events.RegistrationFinishedEvent
import moe.nea.ledger.events.SupplyDebugInfo
import moe.nea.ledger.modules.ExternalDataProvider
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Mouse

class ItemIdProvider {

	@SubscribeEvent
	fun onMouseInput(event: GuiScreenEvent.MouseInputEvent.Pre) {
		if (Mouse.getEventButton() == -1) return
		MinecraftForge.EVENT_BUS.post(BeforeGuiAction(event.gui))
	}

	@SubscribeEvent
	fun onKeyInput(event: GuiScreenEvent.KeyboardInputEvent.Pre) {
		MinecraftForge.EVENT_BUS.post(BeforeGuiAction(event.gui))
	}

	private val knownNames = mutableMapOf<String, ItemId>()

	@SubscribeEvent
	fun onDataLoaded(event: ExternalDataProvider.DataLoaded) {
		event.provider.itemNames.forEach { (itemId, itemName) ->
			knownNames[itemName.unformattedString().trim()] = ItemId(itemId)
		}
	}

	@SubscribeEvent
	fun onRegistrationFinished(event: RegistrationFinishedEvent) {
		MinecraftForge.EVENT_BUS.post(ExtraSupplyIdEvent(knownNames::put))
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	fun savePlayerInventoryIds(event: BeforeGuiAction) {
		val player = Minecraft.getMinecraft().thePlayer ?: return
		val inventory = player.inventory ?: return
		inventory.mainInventory?.forEach { saveFromSlot(it) }
		inventory.armorInventory?.forEach { saveFromSlot(it) }
	}

	@SubscribeEvent
	fun onDebugData(event: SupplyDebugInfo) {
		event.record("knownItemNames", knownNames.size)
	}

	fun saveFromSlot(stack: ItemStack?, preprocessName: (String) -> String = { it }) {
		if (stack == null) return
		val nbt = stack.tagCompound ?: NBTTagCompound()
		val display = nbt.getCompoundTag("display")
		var name = display.getString("Name").unformattedString()
		name = preprocessName(name)
		name = name.trim()
		val id = stack.getInternalId()
		if (id != null && name.isNotBlank()) {
			knownNames[name] = id
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	fun saveChestInventoryIds(event: BeforeGuiAction) {
		val slots = event.chestSlots ?: return
		val chestName = slots.lowerChestInventory.name.unformattedString()
		val isOrderMenu = chestName == "Your Bazaar Orders" || chestName == "Co-op Bazaar Orders"
		val preprocessor: (String) -> String = if (isOrderMenu) {
			{ it.removePrefix("BUY ").removePrefix("SELL ") }
		} else {
			{ it }
		}
		slots.inventorySlots.forEach {
			saveFromSlot(it?.stack, preprocessor)
		}
	}

	// TODO: make use of colour
	fun findForName(name: String, fallbackToGenerated: Boolean = true): ItemId? {
		var id = knownNames[name]
		if (id == null && fallbackToGenerated) {
			id = generateName(name)
		}
		return id
	}

	fun generateName(name: String): ItemId {
		return ItemId(name.uppercase().replace(" ", "_"))
	}

	private val coinRegex = "(?<amount>$SHORT_NUMBER_PATTERN) Coins?".toPattern()
	private val stackedItemRegex = "(?<name>.*) x(?<count>$SHORT_NUMBER_PATTERN)".toPattern()
	private val essenceRegex = "(?<essence>.*) Essence x(?<count>$SHORT_NUMBER_PATTERN)".toPattern()

	fun findCostItemsFromSpan(lore: List<String>): List<Pair<ItemId, Double>> {
		return lore.iterator().asSequence()
			.dropWhile { it.unformattedString() != "Cost" }.drop(1)
			.takeWhile { it != "" }
			.map { findStackableItemByName(it) ?: Pair(ItemId.NIL, 1.0) }
			.toList()
	}

	private val etherialRewardPattern = "\\+(?<amount>${SHORT_NUMBER_PATTERN})x? (?<what>.*)".toPattern()

	fun findStackableItemByName(name: String, fallbackToGenerated: Boolean = false): Pair<ItemId, Double>? {
		val properName = name.unformattedString().trim()
		if (properName == "FREE" || properName == "This Chest is Free!") {
			return Pair(ItemId.COINS, 0.0)
		}
		coinRegex.useMatcher(properName) {
			return Pair(ItemId.COINS, parseShortNumber(group("amount")))
		}
		etherialRewardPattern.useMatcher(properName) {
			val id = when (val id = group("what")) {
				"Copper" -> ItemId.COPPER
				"Bits" -> ItemId.BITS
				"Garden Experience" -> ItemId.GARDEN
				"Farming XP" -> ItemId.FARMING
				"Gold Essence" -> ItemId.GOLD_ESSENCE
				"Gemstone Powder" -> ItemId.GEMSTONE_POWDER
				"Mithril Powder" -> ItemId.MITHRIL_POWDER
				"Pelts" -> ItemId.PELT
				"Fine Flour" -> ItemId.FINE_FLOUR
				else -> {
					id.ifDropLast(" Experience") {
						ItemId.skill(generateName(it).string)
					} ?: id.ifDropLast(" XP") {
						ItemId.skill(generateName(it).string)
					} ?: id.ifDropLast(" Powder") {
						ItemId("SKYBLOCK_POWDER_${generateName(it).string}")
					} ?: id.ifDropLast(" Essence") {
						ItemId("ESSENCE_${generateName(it).string}")
					}  ?: generateName(id)
				}
			}
			return Pair(id, parseShortNumber(group("amount")))
		}
		essenceRegex.useMatcher(properName) {
			return Pair(ItemId("ESSENCE_${group("essence").uppercase()}"),
			            parseShortNumber(group("count")))
		}
		stackedItemRegex.useMatcher(properName) {
			var item = findForName(group("name"), fallbackToGenerated)
			if (item != null) {
				val count = parseShortNumber(group("count"))
				return Pair(item, count)
			}
		}
		return findForName(properName, fallbackToGenerated)?.let { Pair(it, 1.0) }
	}
}