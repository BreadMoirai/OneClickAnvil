package com.github.breadmoirai.oneclickanvil.client;

import com.github.breadmoirai.oneclickanvil.config.OneClickAnvilConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.ArrayList;

@Environment(EnvType.CLIENT)
public class OneClickAnvilClient implements ClientModInitializer, ClientTickEvents.StartTick, ScreenEvents.AfterInit, ScreenEvents.Remove {

    private static OneClickAnvilClient INSTANCE;
    public static OneClickAnvilClient getInstance() {
        return INSTANCE;
    }

    private OneClickAnvilConfig config;
    private ArrayList<Runnable> ops;

    private boolean is_renaming;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        OneClickAnvilConfig.loadModConfig();
        config = OneClickAnvilConfig.getInstance();
        ops = new ArrayList<>();
        ScreenEvents.AFTER_INIT.register(this);
        ClientTickEvents.START_CLIENT_TICK.register(this);
        is_renaming = false;
    }

    public void onAnvilScreenSlotUpdate(AnvilScreen anvil, ScreenHandler handler, int slotId, ItemStack stack) {
        if (config.getItem().isEmpty() || config.getRename().isEmpty()) return;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        ClientPlayerInteractionManager interactionManager = MinecraftClient.getInstance().interactionManager;
        if (interactionManager == null) return;
        if (slotId == 0) {
            if (stack.getCustomName() != null) return;
            if (!stackMatchesItemToRename(stack)) return;
            if (stack.getCount() != stack.getMaxCount()) return;
            ops.add(() -> {
                this.is_renaming = true;
                anvil.nameField.setText(config.getRename());
            });
        }
        if (slotId == 2) {
            if (!this.is_renaming) {
                return;
            }
            if (stack.isEmpty()) {
                ops.add(() -> {
                    for (Slot slot : anvil.getScreenHandler().slots) {
                        if (slot.inventory != player.getInventory()) continue;
                        if (!slot.hasStack()) continue;
                        ItemStack nextStack = slot.getStack();
                        if (!stackMatchesItemToRename(nextStack)) continue;
                        if (nextStack.getCustomName() != null) continue;
                        if (nextStack.getCount() != nextStack.getMaxCount()) continue;
                        interactionManager.clickSlot(anvil.getScreenHandler().syncId, slot.id, 0, SlotActionType.QUICK_MOVE, player);
                        return;
                    }
                    this.is_renaming = false;
                });
            } else {
                Text customName = stack.getCustomName();
                if (customName == null) return;
                if (config.getRename().equals(customName.getLiteralString())) {
                    ops.add(() -> interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.QUICK_MOVE, player));
                }
            }
        }
    }

    private boolean stackMatchesItemToRename(ItemStack stack) {
        return Registries.ITEM.getId(stack.getItem()).toString().equals(config.getItem());
    }

    @Override
    public void onStartTick(MinecraftClient minecraftClient) {
        if (ops.isEmpty()) return;
        ops.removeFirst().run();
    }

    @Override
    public void afterInit(MinecraftClient minecraftClient, Screen screen, int i, int i1) {
        ScreenEvents.remove(screen).register(this);
    }

    @Override
    public void onRemove(Screen screen) {
        ops.clear();
    }
}
