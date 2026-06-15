package com.github.breadmoirai.oneclickanvil.mixin;
import com.github.breadmoirai.oneclickanvil.client.OneClickAnvilClient;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilScreen.class)
public class AnvilScreenMixin {
   @Inject(at = @At("TAIL"), method = "slotChanged(Lnet/minecraft/world/inventory/AbstractContainerMenu;ILnet/minecraft/world/item/ItemStack;)V")
   private void onScreenHandlerSlotUpdate(AbstractContainerMenu handler, int slotId, ItemStack stack, CallbackInfo ci) {
      OneClickAnvilClient.getInstance().onAnvilScreenSlotUpdate((AnvilScreen) (Object) this, handler, slotId, stack);
   }
}
