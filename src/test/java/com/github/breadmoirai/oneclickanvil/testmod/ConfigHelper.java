package com.github.breadmoirai.oneclickanvil.testmod;

import com.terraformersmc.modmenu.gui.ModsScreen;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.gui.YACLScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Drives the mod's YACL config screen via ModMenu, purely through simulated UI. The
 * ModMenu mod-list row height is a fixed 36px; the {@code mouseClicked} signature changed
 * in 1.21.9 (now takes a {@code MouseButtonEvent}), handled via a Stonecutter condition.
 */
@SuppressWarnings("UnstableApiUsage")
public class ConfigHelper {

   // YACL option / button labels (translated strings from en_us.json)
   public static final String LABEL_ITEM = "Item";
   public static final String LABEL_RENAME = "Rename";
   public static final String LABEL_ENABLED = "Enabled";
   public static final String LABEL_ADD_ENTRY = "Add Entry";
   public static final String LABEL_REMOVE_ENTRY = "Remove Entry";

   private static final String MOD_ID = "one-click-anvil";

   private final ClientGameTestContext context;

   public ConfigHelper(ClientGameTestContext context) {
      this.context = context;
   }

   /**
    * The currently displayed screen. MC 26.2 removed the public {@code Minecraft.screen} field;
    * the current screen now lives on {@code Minecraft.gui} and is read via {@code Gui.screen()}.
    */
   public static Screen currentScreen(Minecraft mc) {
      //? if >=26.2 {
      return mc.gui.screen();
      //? } else {
      /*return mc.screen;
      *///? }
   }

   // -------------------------------------------------------------------------
   // Navigation
   // -------------------------------------------------------------------------

   public void openConfigViaModMenu() {
      context.setScreen(() -> new ModsScreen(null));
      context.waitForScreen(ModsScreen.class);

      int[] entryCenter = context.computeOnClient(mc -> {
         ModsScreen screen = (ModsScreen) currentScreen(mc);
         try {
            Field modListField = ModsScreen.class.getDeclaredField("modList");
            modListField.setAccessible(true);
            Object modList = modListField.get(screen);
            @SuppressWarnings("unchecked")
            List<Object> children = (List<Object>) modList.getClass()
               .getMethod("children").invoke(modList);

            for (int i = 0; i < children.size(); i++) {
               Object entry = children.get(i);
               Object mod = entry.getClass().getMethod("getMod").invoke(entry);
               String id = (String) mod.getClass().getMethod("getId").invoke(mod);
               if (MOD_ID.equals(id)) {
                  int rowTop = (int) modList.getClass()
                     .getMethod("getRowTop", int.class).invoke(modList, i);
                  int rowHeight = 36; // ModMenu 15.x fixed row height
                  int listX = (int) modList.getClass().getMethod("getX").invoke(modList);
                  int listWidth = (int) modList.getClass().getMethod("getWidth").invoke(modList);
                  double scale = mc.getWindow().getGuiScale();
                  int guiX = listX + listWidth / 2;
                  int guiY = rowTop + rowHeight / 2;
                  return new int[]{(int) (guiX * scale), (int) (guiY * scale)};
               }
            }
            throw new AssertionError("Mod '" + MOD_ID + "' not found in ModsScreen list");
         } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
         }
      });

      context.getInput().setCursorPos(entryCenter[0], entryCenter[1]);
      context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
      context.waitTick();

      context.runOnClient(mc -> {
         ModsScreen screen = (ModsScreen) currentScreen(mc);
         try {
            Field configField = ModsScreen.class.getDeclaredField("configureButton");
            configField.setAccessible(true);
            AbstractWidget btn = (AbstractWidget) configField.get(screen);
            if (btn == null || !btn.active) {
               throw new AssertionError("Configure button is null or inactive for " + MOD_ID);
            }
            double cx = btn.getX() + btn.getWidth() / 2.0;
            double cy = btn.getY() + btn.getHeight() / 2.0;
            //? if >=1.21.9 {
            btn.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(cx, cy,
               new net.minecraft.client.input.MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0)), false);
            //? } else {
            /*btn.mouseClicked(cx, cy, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            *///? }
         } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
         }
      });
      context.waitForScreen(YACLScreen.class);
   }

   /** Clicks "Save Changes" (if present) then "Done", and waits until the YACL screen closes. */
   public void saveAndCloseYacl() {
      context.tryClickScreenButton("yacl.gui.save");
      context.clickScreenButton("gui.done");
      context.waitFor(mc -> !(currentScreen(mc) instanceof YACLScreen));
   }

   public void closeModsScreen() {
      context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
      context.waitFor(mc -> currentScreen(mc) == null || currentScreen(mc) instanceof TitleScreen);
   }

   // -------------------------------------------------------------------------
   // YACL option helpers (call while a YACLScreen is open)
   // -------------------------------------------------------------------------

   /** Invokes a {@link ButtonOption}'s action (e.g. "Add Entry"), which rebuilds the screen. */
   public void clickYaclButton(String label) {
      context.runOnClient(mc -> {
         YACLScreen screen = (YACLScreen) currentScreen(mc);
         for (ConfigCategory category : screen.config.categories()) {
            for (OptionGroup group : category.groups()) {
               for (Option<?> option : group.options()) {
                  if (option instanceof ButtonOption button
                     && button.name().getString().equals(label)) {
                     button.action().accept(screen, button);
                     return;
                  }
               }
            }
         }
         throw new AssertionError("ButtonOption '" + label + "' not found");
      });
      context.waitForScreen(YACLScreen.class);
      context.waitTick();
   }

   public void setYaclString(String label, String value) {
      setOption(label, value);
   }

   public void setYaclBoolean(String label, boolean value) {
      setOption(label, value);
   }

   public String getYaclString(String label) {
      return context.computeOnClient(mc -> {
         @SuppressWarnings("unchecked")
         Option<String> option = (Option<String>) findOption((YACLScreen) currentScreen(mc), label);
         return option.pendingValue();
      });
   }

   public boolean getYaclBoolean(String label) {
      return context.computeOnClient(mc -> {
         @SuppressWarnings("unchecked")
         Option<Boolean> option = (Option<Boolean>) findOption((YACLScreen) currentScreen(mc), label);
         return option.pendingValue();
      });
   }

   private <T> void setOption(String label, T value) {
      context.runOnClient(mc -> {
         @SuppressWarnings("unchecked")
         Option<T> option = (Option<T>) findOption((YACLScreen) currentScreen(mc), label);
         option.requestSet(value);
      });
      context.waitTick();
   }

   private static Option<?> findOption(YACLScreen screen, String label) {
      for (ConfigCategory category : screen.config.categories()) {
         for (OptionGroup group : category.groups()) {
            for (Option<?> option : group.options()) {
               if (option.name().getString().equals(label)) {
                  return option;
               }
            }
         }
      }
      throw new AssertionError("YACL option '" + label + "' not found");
   }
}
