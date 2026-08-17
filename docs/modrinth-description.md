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
