package com.github.breadmoirai.oneclickanvil.testmod;

import com.github.breadmoirai.oneclickanvil.config.OneClickAnvilConfig;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base for OneClickAnvil client gametests. Provides world setup, command helpers, anvil
 * interaction and inventory assertions. Tests drive behaviour through the real anvil UI and
 * server commands; the mod's own classes are touched only to seed config entries (the config
 * UI itself is covered separately via {@link ConfigHelper}).
 */
@SuppressWarnings("UnstableApiUsage")
public abstract class TestSuite {

   protected final ClientGameTestContext context;
   protected final TestSingleplayerContext world;

   protected TestSuite(ClientGameTestContext context, TestSingleplayerContext world) {
      this.context = context;
      this.world = world;
   }

   public static TestSingleplayerContext createTestWorld(ClientGameTestContext context) {
      TestSingleplayerContext world = context.worldBuilder()
         .setUseConsistentSettings(true)
         .create();
      world.getClientLevel().waitForChunksDownload();
      // @a required — runCommand runs as the server console (@s = server, not player)
      world.getServer().runCommand("time set day");
      // Creative so anvil renames are free (no XP cost) and items are unlimited.
      world.getServer().runCommand("gamemode creative @a");
      context.waitTick();
      return world;
   }

   // -------------------------------------------------------------------------
   // Server command helpers
   // -------------------------------------------------------------------------

   protected void clearInventory() {
      world.getServer().runCommand("clear @a");
      context.waitTick();
   }

   protected void giveItem(String itemStackArg) {
      world.getServer().runCommand("give @a " + itemStackArg);
      context.waitTick();
   }

   // -------------------------------------------------------------------------
   // Screen helpers
   // -------------------------------------------------------------------------

   /** Places an anvil next to the player and right-clicks it to open the anvil screen. */
   protected void openAnvil() {
      BlockPos playerPos = context.computeOnClient(mc -> {
         assert mc.player != null;
         return mc.player.blockPosition();
      });
      int sx = playerPos.getX() + 1;
      int sy = playerPos.getY();
      int sz = playerPos.getZ();
      world.getServer().runCommand(
         "execute unless block %d %d %d minecraft:anvil run setblock %d %d %d minecraft:anvil"
            .formatted(sx, sy, sz, sx, sy, sz));
      context.waitTick();

      BlockPos anvilPos = new BlockPos(sx, sy, sz);
      context.runOnClient(mc -> {
         BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(anvilPos), Direction.WEST, anvilPos, false);
         assert mc.gameMode != null;
         mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
      });
      context.waitForScreen(AnvilScreen.class);
   }

   protected void closeScreen() {
      context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
      context.waitFor(mc -> mc.screen == null);
   }

   /**
    * Shift-clicks (quick-moves) the first inventory stack matching {@code itemId} from the
    * player inventory into the currently open container — i.e. into the anvil input slot.
    */
   protected void quickMoveFromInventory(String itemId) {
      context.runOnClient(mc -> {
         LocalPlayer player = mc.player;
         assert player != null && mc.gameMode != null;
         AbstractContainerMenu menu = player.containerMenu;
         for (Slot slot : menu.slots) {
            if (slot.container != player.getInventory()) continue;
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) continue;
            if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)) continue;
            mc.gameMode.handleContainerInput(
               menu.containerId, slot.index, 0, ContainerInput.QUICK_MOVE, player);
            return;
         }
         throw new AssertionError("No stack of " + itemId + " in inventory to quick-move");
      });
      context.waitTick();
   }

   // -------------------------------------------------------------------------
   // Config seeding (singleton — config UI is tested separately)
   // -------------------------------------------------------------------------

   protected void setSingleConfigEntry(String item, String rename, boolean enabled) {
      context.runOnClient(mc -> {
         OneClickAnvilConfig cfg = OneClickAnvilConfig.getInstance();
         clearEntries(cfg);
         cfg.addEntry();
         OneClickAnvilConfig.Entry entry = cfg.getEntries().getLast();
         entry.setItem(item);
         entry.setRename(rename);
         entry.setEnabled(enabled);
      });
   }

   protected void clearConfigEntries() {
      context.runOnClient(mc -> clearEntries(OneClickAnvilConfig.getInstance()));
   }

   private static void clearEntries(OneClickAnvilConfig cfg) {
      // getEntries() returns a copy of the list (but the same Entry references), so it is
      // safe to iterate it while removing from the backing list.
      for (OneClickAnvilConfig.Entry e : cfg.getEntries()) {
         cfg.removeEntry(e);
      }
   }

   // -------------------------------------------------------------------------
   // Inventory assertions
   // -------------------------------------------------------------------------

   protected void assertInventoryContainsNamed(String itemId, String customName) {
      try {
         context.waitFor(mc -> hasNamed(mc, itemId, customName), 20);
      } catch (AssertionError timeout) {
         throw new AssertionError(
            "Expected inventory to contain " + itemId + " named '" + customName
               + "', but found " + snapshotNames(itemId));
      }
   }

   /**
    * Waits until exactly {@code expected} stacks of {@code itemId} carry {@code customName}.
    * Used to verify the multi-stack auto-rename chain finishes renaming every matching stack.
    * The timeout is generous because each stack costs several ticks (op queue + server round-trip).
    */
   protected void assertInventoryCountNamed(String itemId, String customName, int expected) {
      try {
         context.waitFor(mc -> countNamed(mc, itemId, customName) == expected, 300);
      } catch (AssertionError timeout) {
         int actual = context.computeOnClient(mc -> countNamed(mc, itemId, customName));
         throw new AssertionError(
            "Expected " + expected + " stacks of " + itemId + " named '" + customName
               + "', but found " + actual + " " + snapshotNames(itemId)
               + " | menu: " + snapshotMenu());
      }
   }

   protected void assertInventoryNotNamed(String itemId, String customName) {
      context.waitTick();
      boolean found = context.computeOnClient(mc -> hasNamed(mc, itemId, customName));
      if (found) {
         throw new AssertionError(
            "Expected NO " + itemId + " named '" + customName + "' in inventory, but found one");
      }
   }

   private static boolean hasNamed(Minecraft mc, String itemId, String customName) {
      if (mc.player == null) return false;
      Inventory inv = mc.player.getInventory();
      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack stack = inv.getItem(i);
         if (stack.isEmpty()) continue;
         if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)) continue;
         Component name = stack.get(DataComponents.CUSTOM_NAME);
         if (name != null && name.getString().equals(customName)) return true;
      }
      return false;
   }

   private static int countNamed(Minecraft mc, String itemId, String customName) {
      if (mc.player == null) return 0;
      Inventory inv = mc.player.getInventory();
      int count = 0;
      for (int i = 0; i < inv.getContainerSize(); i++) {
         ItemStack stack = inv.getItem(i);
         if (stack.isEmpty()) continue;
         if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)) continue;
         Component name = stack.get(DataComponents.CUSTOM_NAME);
         if (name != null && name.getString().equals(customName)) count++;
      }
      return count;
   }

   /** Dumps the open menu's anvil slots (0/1 inputs, 2 output) plus any non-empty inventory slots. */
   private String snapshotMenu() {
      return context.computeOnClient(mc -> {
         if (mc.player == null) return "<no player>";
         AbstractContainerMenu menu = mc.player.containerMenu;
         StringBuilder sb = new StringBuilder();
         for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            boolean inv = slot.container == mc.player.getInventory();
            ItemStack s = slot.getItem();
            if (inv && s.isEmpty()) continue; // always show the 3 anvil slots; skip empty inv slots
            Component name = s.get(DataComponents.CUSTOM_NAME);
            sb.append(i < 3 ? "ANVIL#" : "inv#").append(i).append('=')
               .append(s.isEmpty() ? "<empty>"
                  : BuiltInRegistries.ITEM.getKey(s.getItem()) + " x" + s.getCount()
                     + (name == null ? " <unnamed>" : " '" + name.getString() + "'"))
               .append("  ");
         }
         return sb.toString();
      });
   }

   private Map<String, String> snapshotNames(String itemId) {
      return context.computeOnClient(mc -> {
         Map<String, String> names = new LinkedHashMap<>();
         if (mc.player == null) return names;
         Inventory inv = mc.player.getInventory();
         for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)) continue;
            Component name = stack.get(DataComponents.CUSTOM_NAME);
            names.put("slot" + i, name == null ? "<unnamed>" : name.getString());
         }
         return names;
      });
   }

   // -------------------------------------------------------------------------
   // Misc
   // -------------------------------------------------------------------------

   protected void wait(int ticks) {
      context.waitTicks(ticks);
   }
}
