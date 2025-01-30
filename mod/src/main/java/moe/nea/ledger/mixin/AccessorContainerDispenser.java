package moe.nea.ledger.mixin;

import net.minecraft.inventory.ContainerDispenser;
import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ContainerDispenser.class)
public interface AccessorContainerDispenser {
	@Accessor("dispenserInventory")
	IInventory getDispenserInventory_ledger();
}
