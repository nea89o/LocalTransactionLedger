package moe.nea.ledger.mixin;

import net.minecraft.inventory.ContainerDispenser;
import net.minecraft.inventory.ContainerHopper;
import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ContainerHopper.class)
public interface AccessorContainerHopper {
	@Accessor("hopperInventory")
	IInventory getHopperInventory_ledger();
}
