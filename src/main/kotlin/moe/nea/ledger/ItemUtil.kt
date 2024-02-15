package moe.nea.ledger

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound


fun ItemStack.getInternalId(): String? {
    val nbt = this.tagCompound ?: NBTTagCompound()
    val extraAttributes = nbt.getCompoundTag("ExtraAttributes")
    val id = extraAttributes.getString("id")
    return id.takeIf { it.isNotBlank() }
}

