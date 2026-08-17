# Simple Debug Stick

**The debug stick for survival — craftable, and locked to exactly the block states you allow.**

The vanilla debug stick is a creative-only, operator-only tool. Simple Debug Stick makes it
**craftable in survival** and puts the server operator in control: a single config file
decides which blocks, which properties, and even which *values* a normal player may edit.
Operators and creative players keep the unrestricted vanilla item.

One project, two ways to run it:

- **Fabric / Quilt mod** — Minecraft **1.18 → 26.2** (every version, no gaps).
- **Server plugin** — one jar for **Bukkit / Spigot / Paper / Purpur / Folia**, Minecraft **1.16+**.

Both read the **same** `SimpleDebugStick.json` and apply the same rules, so a mod world and
a plugin server behave identically.

---

## ✨ Features

- 🔨 **Craftable.** Upgrade a plain stick to a debug stick at a **smithing table** — netherite
  upgrade template + stick + netherite ingot (on 1.18–1.19, which predate smithing templates,
  it's a plain stick + netherite ingot). A deliberate late-game gate, not a day-one tool.
- 🛡️ **Safe by default.** Ships in blacklist mode: everything is editable *except* the
  properties that let a debug stick dupe items, dupe blocks or break progression — fill
  levels, ages, egg/hatch counts, trial-spawner / vault state, end-portal eyes, waterlogging…
- 🎛️ **Rules at three levels.** Per block, per block tag, or per property globally — with an
  allow-list or a blacklist. A deny always beats an allow of the same specificity.
- 🖱️ **Punch to select, right-click to change.** Vanilla ties property selection to *breaking*
  a block, which a survival player never manages in one click — so a punch does it instead.
  Sneak to cycle backwards. Messages appear on the action bar, exactly like the vanilla item.
- 🔁 **Live reload.** `/reload` (mod) or `/simpledebugstick reload` (plugin) re-reads the config
  with no restart.
- 🖥️ **Server-side.** Other players don't need to install anything.

---

## 🔨 Getting the debug stick

At a **smithing table**:

| Slot | Item |
| --- | --- |
| Template | Netherite Upgrade Smithing Template |
| Base | Stick |
| Addition | Netherite Ingot |
| **Result** | **Debug Stick** |

On the plugin you can also use `/simpledebugstick give`.

---

## ⚙️ Configuration

Config file: `config/SimpleDebugStick.json` (mod) or `plugins/SimpleDebugStick/SimpleDebugStick.json`
(plugin). The shipped default is a safe blacklist:

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

Prefer an **allow-list** (only named blocks editable)? Set `"whitelist": true` and list
blocks/tags under `allowed`, e.g. `{ "id": "slabs", "properties": { "type": ["top", "bottom"] } }`.

**Precedence** — most specific first: exact block → block tag → global `properties` →
`whitelist`. Full walkthrough with examples on the **[wiki](https://github.com/iSlavok/Simple-Debug-Stick/wiki)**.

---

## 🧩 Mod vs. plugin

The Fabric/Quilt mod hooks the debug stick itself, so it can edit **any** block-state property.
The plugin works through Bukkit's block-data API, which exposes a curated (but broad) set of
property types — stairs, slabs, walls, fences, panes, bars, directional, waterlogged, age,
powered, open, and more — enough for the default config and typical use. Both honour the same
config and precedence rules.

**Plugin permissions:** `simpledebugstick.use` (default true) — filtered debug stick;
`simpledebugstick.bypass` (default op) — a *creative* operator gets the unrestricted vanilla
stick; `simpledebugstick.command` (default op) — `/simpledebugstick`.

---

## 📦 Supported versions

**Mod (Fabric/Quilt):** one jar per build anchor, each covering a patch band with no gaps.

| Anchor | Covers | Java |
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

**Plugin:** Minecraft 1.16+ on Bukkit/Spigot/Paper/Purpur/Folia.

---

## 📥 Requirements

**Mod:** [Fabric Loader](https://fabricmc.net/), [Fabric API](https://modrinth.com/mod/fabric-api),
[Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin). The Fabric jar also
runs on Quilt.

**Plugin:** none — just drop it in `plugins/`.

---

## 🔗 Links

- **Source & issues:** <https://github.com/iSlavok/Simple-Debug-Stick>
- **Wiki:** <https://github.com/iSlavok/Simple-Debug-Stick/wiki>
- **License:** MIT
