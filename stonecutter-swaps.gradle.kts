// Per-version symbol swaps for Stonecutter. The shared src/ holds the 1.21.x identifiers;
// each entry promotes them to the newer name when `sc.current.parsed >= version`.
//
// MC 26.1 renamed two inventory symbols (un-obfuscated mappings):
//   net.minecraft.world.inventory.ClickType            -> ContainerInput
//   MultiPlayerGameMode.handleInventoryMouseClick(...) -> handleContainerInput(...)
// Fabric's client-gametest API (5.x, used by 26.1) renamed (test code only):
//   TestSingleplayerContext.getClientWorld()           -> getClientLevel()
// The client-gametest API jumped to 6.x for MC 26.3 and moved the chunk-load/level accessors off
// TestSingleplayerContext onto a new TestServerConnection (test code only):
//   TestSingleplayerContext.getClientLevel()           -> getConnection()
// MC 26.3 also dropped GLFW for SDL, so org.lwjgl.glfw.GLFW is gone from the client classpath; key
// and mouse-button constants now come from com.mojang.blaze3d.platform.InputConstants, which exists
// on every supported version (plain shared code, no swap needed).
//
// Entries are ordered NEWEST FIRST: switching down unwinds the chain in order, so a symbol renamed
// twice (getClientWorld -> getClientLevel -> getConnection) must undo the newest rename first.
//
// MC 26.2 API changes are handled inline (they are not single-token renames, so they can't be
// expressed as swaps): Minecraft.setScreen -> Minecraft.setScreenAndShow (OneClickAnvilConfigScreen),
// and the removed public Minecraft.screen field, now read via Minecraft.gui.screen()
// (net.minecraft.client.gui.Gui), routed through ConfigHelper.currentScreen(Minecraft).
extra["swaps"] = mapOf(
    "26.3" to mapOf(
        "getClientLevel" to "getConnection",
    ),
    "26.1" to mapOf(
        "handleInventoryMouseClick" to "handleContainerInput",
        "ClickType" to "ContainerInput",
        "getClientWorld" to "getClientLevel",
    ),
)
