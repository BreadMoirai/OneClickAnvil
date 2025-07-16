package com.github.breadmoirai.oneclickanvil.mixin;
import com.github.breadmoirai.oneclickanvil.client.OneClickAnvilClient;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreen.class)
public class AnvilScreenMixin {
   @Inject(at = @At("TAIL"), method = "onSlotUpdate(Lnet/minecraft/screen/ScreenHandler;ILnet/minecraft/item/ItemStack;)V")
   private void onScreenHandlerSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack, CallbackInfo ci) {
      OneClickAnvilClient.getInstance().onAnvilScreenSlotUpdate((AnvilScreen) (Object) this, handler, slotId, stack);
   }
}
