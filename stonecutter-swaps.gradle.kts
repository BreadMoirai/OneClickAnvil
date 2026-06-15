// Per-version symbol swaps for Stonecutter. The shared src/ holds the 1.21.x identifiers;
// each entry promotes them to the newer name when `sc.current.parsed >= version`.
//
// MC 26.1 renamed two inventory symbols (un-obfuscated mappings):
//   net.minecraft.world.inventory.ClickType            -> ContainerInput
//   MultiPlayerGameMode.handleInventoryMouseClick(...) -> handleContainerInput(...)
// Fabric's client-gametest API (5.x, used by 26.1) renamed (test code only):
//   TestSingleplayerContext.getClientWorld()           -> getClientLevel()
extra["swaps"] = mapOf(
    "26.1" to mapOf(
        "handleInventoryMouseClick" to "handleContainerInput",
        "ClickType" to "ContainerInput",
        "getClientWorld" to "getClientLevel",
    ),
)
