package moe.nea.ledger

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound


fun ItemStack.getExtraAttributes(): NBTTagCompound {
	val nbt = this.tagCompound ?: return NBTTagCompound()
	return nbt.getCompoundTag("ExtraAttributes")
}

fun ItemStack?.getInternalId(): ItemId? {
	if (this == null) return null
	val extraAttributes = getExtraAttributes()
	var id = extraAttributes.getString("id")
	id = id.takeIf { it.isNotBlank() }
	if (id == "PET") {
		id = getPetId() ?: id
	}
	if (id == "ENCHANTED_BOOK") {
		id = getEnchanments().entries.singleOrNull()?.let {
			"${it.key};${it.value}".uppercase()
		}
	}
	return id?.let(::ItemId)
}

fun ItemStack.getEnchanments(): Map<String, Int> {
	val enchantments = getExtraAttributes().getCompoundTag("enchantments")
	return enchantments.keySet.associateWith { enchantments.getInteger(it) }
}

class PetInfo {
	var type: String? = null
	var tier: String? = null
}

fun ItemStack.getPetId(): String? {
	val petInfoStr = getExtraAttributes().getString("petInfo")
	val petInfo = Ledger.gson.fromJson(petInfoStr, PetInfo::class.java)
	if (petInfo.type == null || petInfo.tier == null) return null
	return petInfo.type + ";" + rarityToIndex(petInfo.tier ?: "")
}

fun rarityToIndex(rarity: String): Int {
	return when (rarity) {
		"COMMON" -> 0
		"UNCOMMON" -> 1
		"RARE" -> 2
		"EPIC" -> 3
		"LEGENDARY" -> 4
		"MYTHIC" -> 5
		else -> -1
	}
}

fun ItemStack.getLore(): List<String> {
	val nbt = this.tagCompound ?: NBTTagCompound()
	val extraAttributes = nbt.getCompoundTag("display")
	val lore = extraAttributes.getTagList("Lore", 8)
	return (0 until lore.tagCount()).map { lore.getStringTagAt(it) }
}


fun ItemStack.getDisplayNameU(): String {
	val nbt = this.tagCompound ?: NBTTagCompound()
	val extraAttributes = nbt.getCompoundTag("display")
	return extraAttributes.getString("Name")
}

