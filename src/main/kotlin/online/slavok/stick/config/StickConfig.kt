package online.slavok.stick.config

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

/**
 * On-disk shape of `config/SimpleDebugStick.json`.
 *
 * [allowed] and [forbidden] each list blocks, tags and bare properties. A block or
 * property that no rule mentions falls through to [whitelist]: `true` denies it,
 * `false` permits it. See [Config] for the exact precedence between the two lists.
 */
data class StickConfig(
    val allowed: Rules,
    val forbidden: Rules,
    val whitelist: Boolean,
) {
    /** One side (`allowed` / `forbidden`) of the rule set. */
    data class Rules(
        val blocks: List<Entry>,
        val properties: Map<String, List<String>>,
        val tags: List<Entry>,
    ) {
        // Every punch and every use runs several rule lookups, each of which used to
        // linear-scan these lists and re-build the namespaced id for every element.
        // Index once instead; the config is immutable between reloads.
        private val blockIndex: Map<String, Entry> by lazy { index(blocks) }
        private val tagIndex: Map<String, Entry> by lazy { index(tags) }

        fun block(id: String): Entry? = blockIndex[id]

        fun tag(id: String): Entry? = tagIndex[id]

        /** `true` when this side globally forbids *every* value of [propertyName]. */
        fun globallyForbidsAll(propertyName: String): Boolean =
            properties[propertyName]?.contains(ALL) == true

        companion object {
            // Duplicate ids are a config typo; keep the first, matching the old
            // first-match-wins linear scan.
            private fun index(entries: List<Entry>): Map<String, Entry> =
                buildMap { entries.forEach { putIfAbsent(it.normalizedId, it) } }

            val CODEC: Codec<Rules> = RecordCodecBuilder.create { instance ->
                instance.group(
                    Codec.list(Entry.CODEC).fieldOf("blocks").forGetter(Rules::blocks),
                    Codec.unboundedMap(Codec.STRING, Codec.list(Codec.STRING))
                        .fieldOf("properties").forGetter(Rules::properties),
                    Codec.list(Entry.CODEC).fieldOf("tags").forGetter(Rules::tags),
                ).apply(instance) { blocks, properties, tags -> Rules(blocks, properties, tags) }
            }
        }
    }

    /**
     * A single block or tag rule. An entry with no [properties] covers the whole
     * block/tag; otherwise it only covers the listed properties (and, per property,
     * the listed values — `["all"]` meaning every value).
     */
    data class Entry(
        val id: String,
        val properties: Map<String, List<String>> = emptyMap(),
    ) {
        /** Config authors may omit the namespace for vanilla blocks and tags. */
        val normalizedId: String = if (':' in id) id else "minecraft:$id"

        val isEmpty: Boolean get() = properties.isEmpty()

        /**
         * Whether this entry *permits* [propertyName] to be selected. Mentioning a
         * property at all is enough — the individual values are checked separately by
         * [coversValue].
         */
        fun allowsProperty(propertyName: String): Boolean =
            properties.containsKey(ALL) || properties.containsKey(propertyName)

        /**
         * Whether this entry *denies* [propertyName] outright. Unlike [allowsProperty]
         * this needs an explicit `"all"`: a rule that names specific values still leaves
         * the property selectable, it just narrows which values it can cycle through.
         */
        fun forbidsProperty(propertyName: String): Boolean =
            properties.containsKey(ALL) || properties[propertyName]?.contains(ALL) == true

        /** Whether this entry names [value] of [propertyName], directly or via `"all"`. */
        fun coversValue(propertyName: String, value: String): Boolean {
            if (properties.containsKey(ALL)) return true
            val values = properties[propertyName] ?: return false
            return values.contains(ALL) || values.contains(value)
        }

        companion object {
            val CODEC: Codec<Entry> = RecordCodecBuilder.create { instance ->
                instance.group(
                    Codec.STRING.fieldOf("id").forGetter(Entry::id),
                    Codec.unboundedMap(Codec.STRING, Codec.list(Codec.STRING))
                        .optionalFieldOf("properties", emptyMap()).forGetter(Entry::properties),
                ).apply(instance) { id, properties -> Entry(id, properties) }
            }
        }
    }

    companion object {
        /** Wildcard key/value accepted anywhere a property name or value is expected. */
        const val ALL = "all"

        val CODEC: Codec<StickConfig> = RecordCodecBuilder.create { instance ->
            instance.group(
                Rules.CODEC.fieldOf("allowed").forGetter(StickConfig::allowed),
                Rules.CODEC.fieldOf("forbidden").forGetter(StickConfig::forbidden),
                Codec.BOOL.fieldOf("whitelist").orElse(true).forGetter(StickConfig::whitelist),
            ).apply(instance) { allowed, forbidden, whitelist ->
                StickConfig(allowed, forbidden, whitelist)
            }
        }

        /**
         * Shipped defaults: decorative block shapes only, and never waterlogging —
         * a survival-safe starting point rather than a free pass.
         */
        fun default(): StickConfig = StickConfig(
            allowed = Rules(
                blocks = listOf(
                    Entry("iron_bars"),
                    Entry(
                        "bamboo",
                        mapOf(
                            "leaves" to listOf("none", "large"),
                            "age" to listOf(ALL),
                        ),
                    ),
                ),
                properties = emptyMap(),
                tags = listOf(
                    Entry("stairs"),
                    Entry("walls"),
                    Entry("c:glass_panes"),
                    Entry("fences"),
                    Entry("slabs", mapOf("type" to listOf("top", "bottom"))),
                ),
            ),
            forbidden = Rules(
                blocks = emptyList(),
                properties = mapOf("waterlogged" to listOf(ALL)),
                tags = emptyList(),
            ),
            whitelist = true,
        )
    }
}
