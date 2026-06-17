# OneClickAnvil — project notes

Fabric mod that auto-applies the anvil rename (one-click anvil). Multi-version via Stonecutter.

## Naming / layout (placeholders for the `backport` and `update` skills)

| Placeholder | Value |
|-------------|-------|
| `<modid>` | `oneclickanvil` |
| `<mod-id>` (mixins.json prefix / jar artifact) | `one-click-anvil` |
| `<modpkg>` | `com.github.breadmoirai.oneclickanvil` |

- **Shared main code:** `src/main/java/com/github/breadmoirai/oneclickanvil/`
  - `mixin/AnvilScreenMixin.java` — the `@Inject` mixin (auto-rename behaviour)
  - `mixin/AnvilScreenAccessor.java` — `@Accessor("name")` interface exposing the `AnvilScreen.name` `EditBox` (replaces the former access widener)
  - `client/OneClickAnvilClient.java` — client entrypoint / logic
  - `config/` — `OneClickAnvilConfig`, `OneClickAnvilConfigScreen`, `OneClickAnvilModMenu` (YACL + ModMenu config UI)
- **Shared test code:** `src/test/java/com/github/breadmoirai/oneclickanvil/testmod/`
  - `TestSuite`, `ConfigHelper`, `AnvilTests`, `OneClickAnvilGameTests` — exercise the anvil auto-rename and the YACL/ModMenu config UI.
- **No access wideners.** Access to `AnvilScreen.name` is provided by the `AnvilScreenAccessor` mixin (above). The previous per-version `oneclickanvil.accesswidener` / `oneclickanviltestmod.accesswidener` files, the `accessWidener` keys in both `fabric.mod.json`s, and the `runningTests` AW-swap block in `build.gradle.kts` / `build.unobf.gradle.kts` have all been removed. The accessor remaps the `name` field automatically across both the 1.21.x (named) and 26.1 (official/un-obfuscated) toolchains — no namespace handling needed.
- **Mixins config:** `versions/<v>/src/main/resources/one-click-anvil.mixins.json` (client array lists `AnvilScreenMixin` and `AnvilScreenAccessor`). These are real per-version files (not symlinks), all identical, with CRLF line endings.

## Versions

- **1.21.x line** (1.21.7, 1.21.8, 1.21.9, 1.21.10, 1.21.11): normal `fabric-loom-remap` + Mojang-mappings path, Java 21, `build.gradle.kts`, `transformUnnamedVars` on switch.
- **26.x line** (26.1, 26.1.1, 26.1.2, 26.2 — `26.2` is the `vcsVersion`; shared `src/` holds its code): un-obfuscated / JDK-25 toolchain. Registered in `settings.gradle.kts` via `versions("26.2", "26.1.2", "26.1.1", "26.1").buildscript("build.unobf.gradle.kts")` (plain `fabric-loom`, no Mojang mappings, `restoreUnnamedVars` on switch, `jar` not `remapJar`). The 26.1.x patches are API-identical to 26.1, so there is no source divergence between them — the `26.1` swap entry covers them all since `sc.current.parsed >= "26.1"`.
- **26.2 API divergences** (handled inline because they are not single-token renames, so they can't be swaps):
  - `Minecraft.setScreen(Screen)` → `Minecraft.setScreenAndShow(Screen)` — inline `//? if >=26.2` block in `config/OneClickAnvilConfigScreen.java`.
  - The public `Minecraft.screen` field was removed; the current screen is now read via `Minecraft.gui.screen()` (`net.minecraft.client.gui.Gui`, with the public `Minecraft.gui` field). Routed through the version-conditional `ConfigHelper.currentScreen(Minecraft)` test helper (a bare `setScreen`/`screen` swap would collide with `ClientGameTestContext.setScreen` and local `screen` vars).
- **Flat, non-versioned layout still holds:** no `vXX_Y` packages. The 26.1 API divergence is handled entirely by **simple identifier swaps** in `stonecutter-swaps.gradle.kts` (shared `src/` keeps the 1.21.x name; the swap promotes it when `current >= 26.1`):
  - `ClickType` → `ContainerInput` (`net.minecraft.world.inventory`)
  - `handleInventoryMouseClick` → `handleContainerInput` (`MultiPlayerGameMode`)
  - `getClientWorld` → `getClientLevel` (Fabric client-gametest `TestSingleplayerContext`, 5.x; test code only)
- `ConfigHelper.java` carries one inline condition (`//? if >=1.21.9 { ... }`) for the `mouseClicked` `MouseButtonEvent` signature change; 26.1 takes the `>=1.21.9` branch.
- The shared `fabric.mod.json` depends only on `fabricloader` + `yet_another_config_lib_v3` (the vestigial `fabric-key-binding-api-v1` hard-dep was removed — unused, and the module no longer exists in 26.1's fabric-api).

## Build (WSL2 / Windows filesystem)

Run via the Windows wrapper (`./gradlew` fails on WSL2). For task names **with spaces**, pass them unwrapped — nesting quotes yields `Task '"Set' not found`:

```bash
cmd.exe /c gradlew.bat :26.1:compileJava
cmd.exe /c gradlew.bat :26.1:runTestClient
cmd.exe /c gradlew.bat "Set active project to 26.1"
```

- `:build` always FAILS (game tests use `runTestClient`, not JUnit) — expected.
- Tests run via `:<version>:runTestClient` (client gametest API, MC >= 1.21.4). 26.1 runs on JDK 25, 1.21.x on JDK 21.
- A "Set active project" or compile occasionally dies with a transient WSL2 I/O error / quick `BUILD FAILED` — just retry (run `./gradlew --stop` first if it persists).

## Adding versions

Use the **`update`** skill for a newer MC version (becomes the new `vcsVersion`) and the **`backport`** skill for an older one. Both read this file for the placeholders above.
