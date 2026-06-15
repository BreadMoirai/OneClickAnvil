package com.github.breadmoirai.oneclickanvil.client;

import com.github.breadmoirai.oneclickanvil.config.OneClickAnvilConfig;
import com.github.breadmoirai.oneclickanvil.mixin.AnvilScreenAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class OneClickAnvilClient implements ClientModInitializer, ClientTickEvents.StartTick, ScreenEvents.AfterInit, ScreenEvents.Remove {

   private static OneClickAnvilClient INSTANCE;

   public static OneClickAnvilClient getInstance() {
      return INSTANCE;
   }

   private OneClickAnvilConfig config;
   private ArrayList<Runnable> ops;

   private String renaming;

   @Override
   public void onInitializeClient() {
      INSTANCE = this;
      OneClickAnvilConfig.loadModConfig();
      config = OneClickAnvilConfig.getInstance();
      ops = new ArrayList<>();
      ScreenEvents.AFTER_INIT.register(this);
      ClientTickEvents.START_CLIENT_TICK.register(this);
      renaming = null;
   }

   public void onAnvilScreenSlotUpdate(AnvilScreen anvil, AbstractContainerMenu handler, int slotId, ItemStack stack) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player == null) return;
      MultiPlayerGameMode interactionManager = Minecraft.getInstance().gameMode;
      if (interactionManager == null) return;
      if (slotId == 0) {
         if (stack.get(DataComponents.CUSTOM_NAME) != null) return;
         if (stack.getCount() != stack.getMaxStackSize()) return;
         getRename(stack).ifPresent(rename -> ops.add(() -> {
            this.renaming = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            ((AnvilScreenAccessor) anvil).getName().setValue(rename);
         }));
      }
      if (slotId == 2) {
         if (this.renaming == null) {
            return;
         }
         if (stack.isEmpty()) {
            String to_rename = this.renaming;
            ops.add(() -> {
               // A slot-2-empty update can fire while the stack being renamed is still in input
               // slot 0 (output not yet computed/pulled). Pulling the next stack now would shift-
               // click it into input slot 1 — which this mod never processes — stranding it there.
               // Only advance once slot 0 is clear; the in-progress rename re-triggers this later.
               if (anvil.getMenu().getSlot(0).hasItem()) return;
               for (Slot slot : anvil.getMenu().slots) {
                  if (slot.container != player.getInventory()) continue;
                  if (!slot.hasItem()) continue;
                  ItemStack nextStack = slot.getItem();
                  if (!to_rename.equals(BuiltInRegistries.ITEM.getKey(nextStack.getItem()).toString())) continue;
                  if (nextStack.get(DataComponents.CUSTOM_NAME) != null) continue;
                  if (nextStack.getCount() != nextStack.getMaxStackSize()) continue;
                  interactionManager.handleContainerInput(anvil.getMenu().containerId, slot.index, 0, ContainerInput.QUICK_MOVE,
                     player);
                  return;
               }
               this.renaming = null;
            });
         } else {
            Component customName = stack.get(DataComponents.CUSTOM_NAME);
            if (customName == null) return;
            getRename(stack).ifPresent(rename -> {
               if (rename.equals(customName.getString())) {
                  ops.add(
                     () -> interactionManager.handleContainerInput(handler.containerId, slotId, 0, ContainerInput.QUICK_MOVE, player));
               }
            });
         }
      }
   }

   private Optional<String> getRename(ItemStack stack) {
      for (OneClickAnvilConfig.Entry entry : config.getEntries()) {
         if (!entry.isEnabled()) continue;
         if (BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(entry.getItem())) {
            return Optional.of(entry.getRename());
         }
      }
      return Optional.empty();
   }

   @Override
   public void onStartTick(Minecraft client) {
      if (ops.isEmpty()) return;
      ops.removeFirst().run();
   }

   @Override
   public void afterInit(Minecraft client, Screen screen, int i, int i1) {
      ScreenEvents.remove(screen).register(this);
   }

   @Override
   public void onRemove(Screen screen) {
      ops.clear();
   }
}
