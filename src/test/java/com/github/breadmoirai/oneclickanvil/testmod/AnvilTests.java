package com.github.breadmoirai.oneclickanvil.testmod;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * OneClickAnvil behaviour tests. Each test seeds a config entry, then exercises the auto-rename
 * feature by interacting with a real anvil screen through simulated clicks and server commands.
 */
@SuppressWarnings("UnstableApiUsage")
public class AnvilTests extends TestSuite {

   private final ConfigHelper config;

   public AnvilTests(ClientGameTestContext context, TestSingleplayerContext world) {
      super(context, world);
      this.config = new ConfigHelper(context);
   }

   public void runAll() {
      // Config UI first, before the behaviour tests start mutating the config singleton.
      configPersistenceViaUi();

      autoRenameHappyPath();
      disabledEntryNoOp();
      nonMatchingItemNoOp();
      alreadyNamedSkipped();
      nonFullStackSkipped();
   }

   /** Suite 1 — add an entry through the YACL UI, save, reopen, verify it round-trips. */
   public void configPersistenceViaUi() {
      clearConfigEntries();
      config.openConfigViaModMenu();
      config.clickYaclButton(ConfigHelper.LABEL_ADD_ENTRY);
      config.setYaclString(ConfigHelper.LABEL_ITEM, "minecraft:diamond_sword");
      config.setYaclString(ConfigHelper.LABEL_RENAME, "Excalibur");
      config.setYaclBoolean(ConfigHelper.LABEL_ENABLED, true);
      config.saveAndCloseYacl();

      config.openConfigViaModMenu();
      check("minecraft:diamond_sword".equals(config.getYaclString(ConfigHelper.LABEL_ITEM)),
         "Item field did not persist across save/reopen");
      check("Excalibur".equals(config.getYaclString(ConfigHelper.LABEL_RENAME)),
         "Rename field did not persist across save/reopen");
      check(config.getYaclBoolean(ConfigHelper.LABEL_ENABLED),
         "Enabled toggle did not persist across save/reopen");
      config.saveAndCloseYacl();
      config.closeModsScreen();
   }

   /** Suite 2 — enabled matching entry renames the item and pulls it into the inventory. */
   public void autoRenameHappyPath() {
      setSingleConfigEntry("minecraft:diamond_sword", "Excalibur", true);
      clearInventory();
      giveItem("minecraft:diamond_sword");
      openAnvil();
      quickMoveFromInventory("minecraft:diamond_sword");
      wait(10);
      assertInventoryContainsNamed("minecraft:diamond_sword", "Excalibur");
      closeScreen();
   }

   /** Suite 3 — disabled entry leaves the item untouched. */
   public void disabledEntryNoOp() {
      setSingleConfigEntry("minecraft:diamond_sword", "Excalibur", false);
      clearInventory();
      giveItem("minecraft:diamond_sword");
      openAnvil();
      quickMoveFromInventory("minecraft:diamond_sword");
      wait(10);
      closeScreen(); // returns the input item to the inventory
      assertInventoryNotNamed("minecraft:diamond_sword", "Excalibur");
   }

   /** Suite 4 — an item with no matching entry is left untouched. */
   public void nonMatchingItemNoOp() {
      setSingleConfigEntry("minecraft:diamond_sword", "Excalibur", true);
      clearInventory();
      giveItem("minecraft:iron_ingot 64");
      openAnvil();
      quickMoveFromInventory("minecraft:iron_ingot");
      wait(10);
      closeScreen();
      assertInventoryNotNamed("minecraft:iron_ingot", "Excalibur");
   }

   /** Suite 5 — an already-named item is skipped (the mod must not re-rename it). */
   public void alreadyNamedSkipped() {
      setSingleConfigEntry("minecraft:diamond_sword", "Excalibur", true);
      clearInventory();
      giveItem("minecraft:diamond_sword[minecraft:custom_name='\"Old Sword\"']");
      openAnvil();
      quickMoveFromInventory("minecraft:diamond_sword");
      wait(10);
      closeScreen();
      assertInventoryNotNamed("minecraft:diamond_sword", "Excalibur");
   }

   /** Suite 6 — a non-full stack is skipped (covers the "filter out non-full stacks" rule). */
   public void nonFullStackSkipped() {
      setSingleConfigEntry("minecraft:dirt", "Fancy Dirt", true);
      clearInventory();
      giveItem("minecraft:dirt 1");
      openAnvil();
      quickMoveFromInventory("minecraft:dirt");
      wait(10);
      closeScreen();
      assertInventoryNotNamed("minecraft:dirt", "Fancy Dirt");
   }

   private static void check(boolean condition, String message) {
      if (!condition) throw new AssertionError(message);
   }
}
