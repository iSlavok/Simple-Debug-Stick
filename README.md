# Simple Debug Stick

**Give the debug stick to survival players — but only over the blocks you allow.**

A server-side Fabric mod for Minecraft **1.21.5 – 1.21.8**. The debug stick becomes
craftable at a smithing table, and a config file decides exactly which blocks,
properties and values a non-operator may edit with it. Operators and creative players
keep the unrestricted vanilla item.

## Features

- 🔨 **Craftable.** Upgrade a plain stick to a debug stick at a **smithing table**:
  netherite upgrade template + stick + netherite ingot. Late-game by design, not a day-one tool.
- 🎛️ **Allow-list by default.** Out of the box only stairs, slabs, walls, fences, glass
  panes, iron bars and bamboo can be edited — and never `waterlogged`.
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

Config file: `config/SimpleDebugStick.json`. Run `/reload` after editing.

```json
{
	"allowed": {
		"blocks": [
			{ "id": "iron_bars" },
			{ "id": "bamboo", "properties": { "leaves": ["none", "large"], "age": ["all"] } }
		],
		"properties": {},
		"tags": [
			{ "id": "stairs" },
			{ "id": "walls" },
			{ "id": "c:glass_panes" },
			{ "id": "fences" },
			{ "id": "slabs", "properties": { "type": ["top", "bottom"] } }
		]
	},
	"forbidden": {
		"blocks": [],
		"properties": { "waterlogged": ["all"] },
		"tags": []
	},
	"whitelist": true
}
```

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

- Minecraft **1.21.5 – 1.21.8**
- [Fabric Loader](https://fabricmc.net/) 0.16+
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin)

## Installation

1. Grab the latest jar from [Releases](https://github.com/iSlavok/Simple-Debug-Stick/releases).
2. Drop it into the server's `mods` folder alongside Fabric API and Fabric Language Kotlin.
3. Start the server once — `config/SimpleDebugStick.json` is created with the defaults.

## Building

```bash
./gradlew build          # jar in build/libs/
./gradlew test           # rule-engine unit tests
./gradlew runGameTest    # headless server: boots the mod and checks the recipe
./gradlew runDatagen     # regenerates src/main/generated (committed)
```

Needs a JDK 21 or newer. The recipe JSON under `src/main/generated` is committed on
purpose, so a plain `build` ships it without running datagen first.

## License

[MIT](LICENSE).
