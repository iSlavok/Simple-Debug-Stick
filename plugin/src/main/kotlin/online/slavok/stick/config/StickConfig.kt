package online.slavok.stick.config

import com.google.gson.Gson
import com.google.gson.JsonElement

/**
 * On-disk shape of the debug-stick config, identical to the Fabric mod's
 * `SimpleDebugStick.json` — the same file works for both. Parsed with Gson here (the
 * plugin has no Minecraft codecs on its classpath), fed to the shared [RuleMatcher].
 */
data class StickConfig(
    val allowed: Rules,
    val forbidden: Rules,
    val whitelist: Boolean,
) {
    data class Rules(
        val blocks: List<Entry>,
        val properties: Map<String, List<String>>,
        val tags: List<Entry>,
    ) {
        // @Transient keeps Gson from serializing these derived indices to the file.
        @delegate:Transient
        private val blockIndex: Map<String, Entry> by lazy { index(blocks) }

        @delegate:Transient
        private val tagIndex: Map<String, Entry> by lazy { index(tags) }

        fun block(id: String): Entry? = blockIndex[id]
        fun tag(id: String): Entry? = tagIndex[id]

        fun globallyForbidsAll(propertyName: String): Boolean =
            properties[propertyName]?.contains(ALL) == true

        companion object {
            private fun index(entries: List<Entry>): Map<String, Entry> =
                buildMap { entries.forEach { putIfAbsent(it.normalizedId, it) } }
        }
    }

    data class Entry(
        val id: String,
        val properties: Map<String, List<String>> = emptyMap(),
    ) {
        // Computed getters (no backing field) so Gson writes only `id` and `properties`.
        val normalizedId: String get() = if (':' in id) id else "minecraft:$id"
        val isEmpty: Boolean get() = properties.isEmpty()

        fun allowsProperty(propertyName: String): Boolean =
            properties.containsKey(ALL) || properties.containsKey(propertyName)

        fun forbidsProperty(propertyName: String): Boolean =
            properties.containsKey(ALL) || properties[propertyName]?.contains(ALL) == true

        fun coversValue(propertyName: String, value: String): Boolean {
            if (properties.containsKey(ALL)) return true
            val values = properties[propertyName] ?: return false
            return values.contains(ALL) || values.contains(value)
        }
    }

    companion object {
        const val ALL = "all"

        /**
         * Shipped defaults — matches the mod. Blacklist mode: everything is editable
         * except the properties that let a debug stick duplicate items, dupe blocks, or
         * break progression (fill levels, ages, egg/hatch counts, trial/vault state,
         * end-portal eyes, waterlogging, ...).
         */
        fun default(): StickConfig = StickConfig(
            allowed = Rules(emptyList(), emptyMap(), emptyList()),
            forbidden = Rules(
                blocks = emptyList(),
                properties = mapOf(
                    "honey_level" to listOf(ALL),
                    "level" to listOf(ALL),
                    "age" to listOf(ALL),
                    "bites" to listOf(ALL),
                    "charges" to listOf(ALL),
                    "eggs" to listOf(ALL),
                    "hatch" to listOf(ALL),
                    "ominous" to listOf(ALL),
                    "trial_spawner_state" to listOf(ALL),
                    "vault_state" to listOf(ALL),
                    "facing" to listOf("end_portal_frame"),
                    "eye" to listOf(ALL),
                    "flower_amount" to listOf(ALL),
                    "waterlogged" to listOf(ALL),
                ),
                tags = emptyList(),
            ),
            whitelist = false,
        )

        // Gson leaves absent fields null, so parse into a lenient DTO and fill defaults.
        private class RawEntry(val id: String?, val properties: Map<String, List<String>>?)
        private class RawRules(
            val blocks: List<RawEntry>?,
            val properties: Map<String, List<String>>?,
            val tags: List<RawEntry>?,
        )
        private class RawConfig(val allowed: RawRules?, val forbidden: RawRules?, val whitelist: Boolean?)

        fun fromJson(gson: Gson, json: JsonElement): StickConfig {
            val raw = gson.fromJson(json, RawConfig::class.java)
            return StickConfig(
                allowed = raw.allowed.toRules(),
                forbidden = raw.forbidden.toRules(),
                whitelist = raw.whitelist ?: true,
            )
        }

        private fun RawRules?.toRules(): Rules = Rules(
            blocks = this?.blocks.orEmpty().map { it.toEntry() },
            properties = this?.properties.orEmpty(),
            tags = this?.tags.orEmpty().map { it.toEntry() },
        )

        private fun RawEntry.toEntry(): Entry =
            Entry(id ?: error("config entry is missing an \"id\""), properties.orEmpty())
    }
}
