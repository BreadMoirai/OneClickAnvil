package com.github.breadmoirai.oneclickanvil.testmod;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/**
 * Entry point for all OneClickAnvil client gametests.
 * Registered via the "fabric-client-gametest" entrypoint in the testmod's fabric.mod.json.
 */
@SuppressWarnings("UnstableApiUsage")
public class OneClickAnvilGameTests implements FabricClientGameTest {

   @Override
   public void runTest(ClientGameTestContext context) {
      try (TestSingleplayerContext world = TestSuite.createTestWorld(context)) {
         new AnvilTests(context, world).runAll();
      }
   }
}
