package moe.nea.ledger.mixin;

import moe.nea.ledger.events.InitializationComplete;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class OnInitializationCompletePatch {

	@Inject(method = "startGame", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/client/FMLClientHandler;onInitializationComplete()V"))
	private void onInitComplete(CallbackInfo ci) {
		MinecraftForge.EVENT_BUS.post(new InitializationComplete());
	}
}
