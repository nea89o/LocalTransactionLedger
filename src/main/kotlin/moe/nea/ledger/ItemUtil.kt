package moe.nea.ledger

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound


fun ItemStack.getInternalId(): String? {
    val nbt = this.tagCompound ?: NBTTagCompound()
    val extraAttributes = nbt.getCompoundTag("ExtraAttributes")
    val id = extraAttributes.getString("id")
    return id.takeIf { it.isNotBlank() }
}


fun ItemStack.getLore(): List<String> {
    val nbt = this.tagCompound ?: NBTTagCompound()
    val extraAttributes = nbt.getCompoundTag("display")
    val lore = extraAttributes.getTagList("Lore", 8)
    return (0 until lore.tagCount()).map { lore.getStringTagAt(it) }
}

