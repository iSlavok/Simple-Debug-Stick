# Simple Debug Stick

**Give the debug stick to survival players — but only over the blocks you allow.**

A server-side Fabric mod for Minecraft **1.18 – 26.2** (built from one source with
[Stonecutter](https://stonecutter.kikugie.dev/); the Fabric jar also runs on Quilt). The
debug stick becomes craftable at a smithing table, and a config file decides exactly which
blocks, properties and values a non-operator may edit with it. Operators and creative
players keep the unrestricted vanilla item.

## Features

- 🔨 **Craftable.** Upgrade a plain stick to a debug stick at a **smithing table**:
  netherite upgrade template + stick + netherite ingot (a plain stick + netherite ingot on
  1.18–1.19, which predate smithing templates). Late-game by design, not a day-one tool.
- 🎛️ **Safe blacklist by default.** Out of the box everything is editable *except* the
  properties that let a debug stick dupe items, dupe blocks or break progression (fill
  levels, ages, egg/hatch counts, trial/vault state, end-portal eyes, `waterlogged`, …).
  Switch to an allow-list, or tighten/loosen it, in the config.
- 🧱 **Rules at three levels.** Per block, per block tag, or per property globally. Deny
  always beats allow at the same level.
- 🖱️ **Punch to select, right-click to change.** Vanilla ties property selection to
  *breaking* a block, which a survival player never manages in one click — so a punch
  does it instead.
- 🔁 **`/reload` re-reads the config.** No server restart to tune the rules.
- 🖥️ **Server-side.** Players do not need the mod installed.

## Usage

1. Craft a debug stick at a smithing table.
2. **Right-click** a block to change the selected property's value.
3. **Punch** the block to select the next property.
4. **Sneak** while doing either to cycle backwards.

Blocked blocks and properties report the vanilla *"… has no properties"* message, so
nothing looks broken to the player.

## Configuration

Config file: `config/SimpleDebugStick.json`. Run `/reload` after editing. The shipped
default (below) is blacklist mode — everything editable except the listed properties:

```json
{
	"allowed": { "blocks": [], "properties": {}, "tags": [] },
	"forbidden": {
		"blocks": [],
		"properties": {
			"honey_level": ["all"], "level": ["all"], "age": ["all"], "bites": ["all"],
			"charges": ["all"], "eggs": ["all"], "hatch": ["all"], "ominous": ["all"],
			"trial_spawner_state": ["all"], "vault_state": ["all"], "eye": ["all"],
			"flower_amount": ["all"], "waterlogged": ["all"]
		},
		"tags": []
	},
	"whitelist": false
}
```

To run an **allow-list** instead (only named blocks editable), set `"whitelist": true` and
list blocks/tags under `allowed`, e.g. `"tags": [{ "id": "stairs" }, { "id": "slabs",
"properties": { "type": ["top", "bottom"] } }]`.

**Options:**

- `whitelist`: what happens to anything no rule mentions. `true` denies it (allow-list
  mode, the default), `false` permits it (deny-list mode).
- `allowed` / `forbidden`: two rule sets of the same shape, each with `blocks`, `tags`
  and `properties`.
- `blocks` / `tags`: a list of `{ "id": ..., "properties": { ... } }` entries. The
  namespace may be omitted for vanilla ids (`stairs` means `minecraft:stairs`). An entry
  with no `properties` covers the whole block or tag; otherwise it covers only the listed
  properties, and per property only the listed values. `"all"` is a wildcard for either.
- `properties`: a global `name → values` map, e.g. *"nobody waterlogs anything"*.

**Precedence** — most specific first: exact block → block tag → global `properties` →
`whitelist`. Within each level `forbidden` is checked before `allowed`.

Full walkthrough with examples: **[the wiki](https://github.com/iSlavok/Simple-Debug-Stick/wiki)**.

## Requirements

- Minecraft **1.18 – 26.2**
- [Fabric Loader](https://fabricmc.net/) 0.14+ (the jar also runs on Quilt)
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)

## Installation

1. Grab the jar for your Minecraft version from
   [Releases](https://github.com/iSlavok/Simple-Debug-Stick/releases).
2. Drop it into the server's `mods` folder alongside Fabric API and Fabric Language Kotlin.
3. Start the server once — `config/SimpleDebugStick.json` is created with the defaults.

## Supported versions

Built from one source with [Stonecutter](https://stonecutter.kikugie.dev/): one jar per
build anchor, each covering a patch band with no gaps. Minecraft 26+ is unobfuscated and
needs JDK 25 to build (the project still builds on JDK 17–24, just without the 26.x nodes).

| Build anchor | Covers | Java |
| --- | --- | --- |
| 1.18.2 | 1.18 – 1.18.2 | 17 |
| 1.19.4 | 1.19 – 1.19.4 | 17 |
| 1.20.4 | 1.20 – 1.20.4 | 17 |
| 1.20.6 | 1.20.5 – 1.20.6 | 21 |
| 1.21.1 | 1.21 – 1.21.1 | 21 |
| 1.21.8 | 1.21.2 – 1.21.8 | 21 |
| 1.21.10 | 1.21.9 – 1.21.11 | 21 |
| 26.1.2 | 26.1.x | 25 |
| 26.2 | 26.2 | 25 |

## Building

```bash
./gradlew build                 # every version node -> versions/<ver>/build/libs/
./gradlew :1.21.8:build         # a single node
./gradlew :1.21.8:runGameTest   # boot a headless server for one node and check the recipe
```

Needs a JDK 21+ to build the yarn anchors; JDK 25 to also include the unobfuscated 26.x
nodes (the Gradle daemon is pinned to 25 via `gradle/gradle-daemon-jvm.properties`). The
per-version recipe JSON under `src/main/recipes/<era>` is committed, so a plain `build`
ships the recipe without a datagen step.

## Server plugin (Bukkit/Spigot/Paper/Purpur/Folia)

For servers without a Fabric client mod, a **Bukkit plugin** reproduces the same
behaviour — one jar for Spigot, Paper, Purpur and Folia (Folia-supported), Minecraft
**1.16+**. It lives in `plugin/` as a standalone build and reads the same
`SimpleDebugStick.json` schema, with identical rule precedence.

- **Give the stick:** the same smithing recipe (on 1.20+ servers) or `/simpledebugstick give`.
- **Edit blocks:** right-click a block to change the selected property, left-click to
  select the next, sneak to reverse — the same UX as the mod.
- **Permissions:** `simpledebugstick.use` (default true) gates the filtered behaviour;
  `simpledebugstick.bypass` (default op) falls through to the unrestricted vanilla stick;
  `simpledebugstick.command` (default op) guards `/simpledebugstick`.
- **Config:** `plugins/SimpleDebugStick/SimpleDebugStick.json`. `/simpledebugstick reload`
  re-reads it.
- Because Bukkit exposes block states through typed `BlockData` interfaces rather than a
  generic property list, the plugin edits a curated set of property types (stairs, slabs,
  walls, fences, panes, bars, directional, waterlogged, age, powered, open, …) — enough to
  cover the default config. The Fabric mod, with full mixin access, is unrestricted.

```bash
./gradlew -p plugin build       # -> plugin/build/libs/simple-debug-stick-plugin-*.jar
./gradlew -p plugin runServer   # boot a real Paper server with the plugin (-Prun_mc=1.21.8)
```

## License

[MIT](LICENSE).
