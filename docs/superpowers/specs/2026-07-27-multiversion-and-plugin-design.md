# Simple Debug Stick — full version coverage + server plugin

## Context

The mod currently builds a single Fabric jar for Minecraft 1.21.5–1.21.8. Two goals:

1. **Cover every Minecraft version 1.18 → 26.2 with no gaps**, as Fabric/Quilt jars, built
   from one source via Stonecutter (the pattern already used by the sibling mods
   Simple-Frames / Simple-Player-Heads).
2. **Port the server-side behaviour to a Bukkit plugin** (Bukkit/Spigot/Paper/Purpur/Folia),
   one jar, the widest Minecraft range the Bukkit API allows.

Delivered as **two PRs**: PR1 = multiversion mod, PR2 = plugin.

Decisions taken: Fabric/Quilt only (no NeoForge for now); pre-1.20 recipe is a legacy
smithing-table upgrade; the plugin covers a curated set of block-state property types via
the typed Bukkit `BlockData` interfaces (one jar, no per-version NMS).

## PR1 — Multiversion mod (Fabric/Quilt)

### Anchors

One jar per anchor; each covers a patch band via its `depends` range. Bands tile the whole
1.18 → 26.2 line with no gap and no overlap.

| Anchor | Covers (`depends`) | Java | State storage | Recipe |
|---|---|---|---|---|
| 1.18.2  | `>=1.18 <1.19`       | 17 | NBT tag | legacy smithing |
| 1.19.4  | `>=1.19 <1.20`       | 17 | NBT tag | legacy smithing |
| 1.20.4  | `>=1.20 <1.20.5`     | 17 | NBT tag | smithing_transform (old JSON) |
| 1.20.6  | `>=1.20.5 <1.21`     | 21 | DataComponent | smithing_transform (new JSON) |
| 1.21.1  | `>=1.21 <1.21.2`     | 21 | DataComponent | smithing_transform (new JSON) |
| 1.21.8  | `>=1.21.2 <1.21.9`   | 21 | DataComponent | smithing_transform (new JSON) |
| 1.21.10 | `>=1.21.9 <1.22`     | 21 | DataComponent | smithing_transform (new JSON) |
| 26.1.2  | `>=26.1 <26.2`       | 25 | DataComponent | smithing_transform (new JSON) |
| 26.2    | `>=26.2 <27`         | 25 | DataComponent | smithing_transform (new JSON) |

Split points follow the APIs the source must branch on: DataComponents (1.20.5), a possible
`use`/`canMine` signature change (1.21.2), the Java-level bumps, and the obfuscated →
unobfuscated boundary (`>=1.22`, i.e. 26+). Per-version dep versions (yarn, fabric-api,
fabric-language-kotlin) are mirrored from the proven Simple-Frames matrix.

### Build

- Gradle 9.5 wrapper (already bumped); `gradle-daemon-jvm.properties` pins the daemon to
  JDK 25 so the 26.x nodes register and 25 cross-compiles the 17/21 targets via
  `options.release`.
- `settings.gradle.kts`: apply `dev.kikugie.stonecutter`, KikuGie maven, `create` block
  with all nine anchors; the two 26.x nodes registered only when `JavaVersion.current >= 25`
  and routed to `build.unobfuscated.gradle.kts`. `vcsVersion = "1.21.8"`.
- `stonecutter.gradle.kts` controller: `stonecutter active "1.21.8"`.
- `build.gradle.kts` (yarn anchors): `Mc` matrix keyed on `stonecutter.current.version`;
  `fabric-loom` + `kotlin("jvm")`; `processResources` expands `version` / `java_level` /
  `minecraft_dep` into `fabric.mod.json` and `*.mixins.json`; per-anchor recipe resource
  dir added to `sourceSets.main.resources` by recipe era.
- `build.unobfuscated.gradle.kts` (26.x): non-remapping `net.fabricmc.fabric-loom`, no
  `mappings`, `implementation`/`jar`, `runtimeOnly` fabric-language-kotlin, JDK 25.

### Source branching

Single `src/`. Stonecutter `//? if <cond> { … //?} else { … //?}` comments only where the
API genuinely differs. The three touch points:

1. **`DebugStickMixin` (Java, stays Java).** The injected `DebugStickItem` interaction
   method drifts in yarn name/signature across versions and is Mojang-named on 26.x. For
   each anchor, confirm the real target by decompiling the mapped jar under
   `~/.gradle/caches/fabric-loom/<ver>/` (non-remapping Loom does *not* validate mixin
   targets at compile time — a wrong target only crashes at class-load, which the gametest
   catches because the mixin is `required`). Branch the `@Inject`/`@Shadow` signatures and,
   critically, the selected-property storage: an NBT compound on `<1.20.5` vs
   `DebugStickStateComponent` on `>=1.20.5`.
2. **`Config` / registry glue (Kotlin).** `RegistryEntry.streamTags`, the block-id lookups,
   and `Identifier` (renamed on 26.x) branch here. The pure rule engine (`RuleMatcher`) does
   not change.
3. **Recipe.** Hand-authored per-era JSON under `src/main/recipes/<era>/data/...`, selected
   by the build matrix — datagen is dropped for the mod (its provider API changes across all
   nine anchors, and the JSON format only has three shapes). Eras: `legacy` (`minecraft:smithing`,
   stick + netherite ingot), `transform-old` (`smithing_transform`, `result:{item}`),
   `transform-new` (`smithing_transform`, `result:{id}`).

### Verification

- Per-anchor gametest (`src/gametest`, gated on anchors whose fabric-api ships a compatible
  gametest API): boots a server, asserts the recipe loads and crafts a debug stick. Booting
  at all proves the `required` mixin applied.
- `RuleMatcher` unit tests stay (version-independent, pure).
- CI `build` job on JDK 25 runs `./gradlew build` (every node + gametests). `release` job on
  a `v*` tag builds all nodes and uploads jars; Modrinth publish wired but dormant until
  `modrinth_id` is set.

## PR2 — Server plugin

### Scope & range

Standalone Gradle build in `plugin/` (own `settings.gradle.kts`), `compileOnly spigot-api`,
one jar for Bukkit/Spigot/Paper/Purpur and Folia (`folia-supported: true`; the interaction
handler is region-local). Minecraft floor 1.13+ (the `BlockData` API); `SmithingTransformRecipe`
registration only where the API exists (1.20+), else the item is given by command. `includeBuild("plugin")`
in the root settings for IDE task visibility only; CI builds it via `-p plugin`.

### Behaviour

- `PlayerInteractEvent` on `minecraft:debug_stick`: right-click a block → change the selected
  property's value; left-click → select the next property; sneak inverts; left-click is
  rate-limited per player (mirrors the mod's punch cooldown). The event is cancelled to
  suppress vanilla handling.
- Block-state editing through the **typed `BlockData` interfaces** — `Stairs`, `Slab`,
  `Bisected`, `Directional`, `MultipleFacing`, `Waterlogged`, `Ageable`, `Powerable`,
  `Openable`, `Rotatable`, `Rail`, … Each handler exposes `(propertyName, orderedValues,
  get, set)`, which feeds the same `RuleMatcher` filtering as the mod. Covers everything in
  the default config.
- Selected property remembered per player+block in an in-memory map (mirrors the vanilla
  component semantics), cleared on quit.
- Permissions: `simpledebugstick.use` (default true) gates the filtered behaviour;
  `simpledebugstick.bypass` (default op) falls through to the unrestricted vanilla stick.

### Config parity

Same `SimpleDebugStick.json` schema, parsed with Gson (no Mojang Codec on a plugin classpath).
`RuleMatcher` is re-expressed as pure plugin-side code over ids + strings; block tags resolve
through `Bukkit.getTag`. The rule precedence is identical to the mod.

### Item delivery

Registers the same smithing recipe where `SmithingTransformRecipe` exists (1.20+) and a
`/simpledebugstick give` command everywhere.

### Testing

- Mockito over the rule engine and the property handlers (no server needed).
- MockBukkit load-smoke (plugin enables, listener + command registered). Main class kept
  non-final for MockBukkit's subclassing; test paper-api version matched to MockBukkit's
  target.
- `./gradlew -p plugin runServer` (run-paper) for manual end-to-end.

### Publishing

Same Modrinth project; loaders `bukkit`/`spigot`/`paper`/`purpur`/`folia`; the plugin's
`gameVersions` tag the full supported Minecraft range.
