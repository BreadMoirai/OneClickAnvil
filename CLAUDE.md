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
- **26.x line** (26.1, 26.1.1, 26.1.2, 26.2, 26.3 — `26.3` is the `vcsVersion`; shared `src/` holds its code): un-obfuscated / JDK-25 toolchain. Registered in `settings.gradle.kts` via `versions("26.3", "26.2", "26.1.2", "26.1.1", "26.1").buildscript("build.unobf.gradle.kts")` (plain `fabric-loom`, no Mojang mappings, `restoreUnnamedVars` on switch, `jar` not `remapJar`). The 26.1.x patches are API-identical to 26.1, so there is no source divergence between them — the `26.1` swap entry covers them all since `sc.current.parsed >= "26.1"`.
- **26.2 API divergences** (handled inline because they are not single-token renames, so they can't be swaps):
  - `Minecraft.setScreen(Screen)` → `Minecraft.setScreenAndShow(Screen)` — inline `//? if >=26.2` block in `config/OneClickAnvilConfigScreen.java`.
  - The public `Minecraft.screen` field was removed; the current screen is now read via `Minecraft.gui.screen()` (`net.minecraft.client.gui.Gui`, with the public `Minecraft.gui` field). Routed through the version-conditional `ConfigHelper.currentScreen(Minecraft)` test helper (a bare `setScreen`/`screen` swap would collide with `ClientGameTestContext.setScreen` and local `screen` vars).
- **26.3 API divergences:**
  - **GLFW is gone — MC 26.3 windows through SDL** (`org.lwjgl:lwjgl-sdl` replaces `lwjgl-glfw` in the
    version manifest), so `org.lwjgl.glfw.GLFW` no longer resolves. The test code's key/mouse
    constants now come from `com.mojang.blaze3d.platform.InputConstants`
    (`KEY_ESCAPE`, `MOUSE_BUTTON_LEFT`), which exists unchanged on every supported version — plain
    shared code, no condition or swap.
  - **Fabric client-gametest API 6.x** (26.3's fabric-api pulls `fabric-client-gametest-api-v1 6.0.7`)
    dropped `TestClientLevelContext`: `TestSingleplayerContext.getClientLevel()` became
    `getConnection()` (`TestServerConnection`, which is what now carries `waitForChunksDownload()`,
    `getClientLevel()`, `getServerLevel()`, ...). Handled by a `"26.3"` swap entry.
  - The swap map is ordered **newest first** so the reverse chain unwinds correctly: `getClientWorld`
    -> `getClientLevel` (26.1) -> `getConnection` (26.3) has to be undone newest-rename-first when
    switching down to 1.21.x.
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
- Running **all** versions: `cmd.exe /c gradlew.bat runTestClient --max-workers=4`. **Cap at 4** —
  each task launches a real Minecraft client, and beyond about four in parallel they contend for
  GPU/window resources and some clients hang permanently (the build never finishes; the stuck JVMs
  have to be killed with `taskkill /F /IM java.exe /T`, which also takes out the Gradle daemons).
- **26.x Windows JVM stack crash (fixed):** `build.unobf.gradle.kts` passes
  `-XX:+UnlockDiagnosticVMOptions -XX:+AlwaysPreTouchStacks` to every loom run. Without it, 10–50% of
  26.x client launches die in the first resource reload (`NTSTATUS 0xC0000005`, no `hs_err`) from a
  HotSpot-on-Windows stack-growth bug — not mod code. Do not remove the flag. Full write-up: `update`
  skill → `references/api-divergences.md` → "26.x client startup crash on Windows".
- A "Set active project" or compile occasionally dies with a transient WSL2 I/O error / quick `BUILD FAILED` — just retry (run `./gradlew --stop` first if it persists).

## Adding versions

Use the **`update`** skill for a newer MC version (becomes the new `vcsVersion`) and the **`backport`** skill for an older one. Both read this file for the placeholders above.
