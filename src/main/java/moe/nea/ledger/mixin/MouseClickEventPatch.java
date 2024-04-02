package moe.nea.ledger.mixin;

import moe.nea.ledger.GuiClickEvent;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public class MouseClickEventPatch {
    @Inject(method = "handleMouseClick", at = @At("HEAD"))
    private void onHandleMouseClick(Slot slotIn, int slotId, int clickedButton, int clickType, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new GuiClickEvent(slotIn, slotId, clickedButton, clickType));
    }
}
