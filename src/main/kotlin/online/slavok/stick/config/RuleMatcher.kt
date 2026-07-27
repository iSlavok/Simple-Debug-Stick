package online.slavok.stick.config

import online.slavok.stick.config.StickConfig.Companion.ALL

/**
 * The rule engine, expressed purely over ids and names so it can be reasoned about (and
 * tested) without a running game. [Config] supplies the registry lookups.
 *
 * Precedence, most specific first:
 *
 * 1. an entry for the exact block,
 * 2. an entry for one of the block's tags,
 * 3. the global `properties` map,
 * 4. `whitelist` — `true` denies anything unmentioned, `false` permits it.
 *
 * Within each level `forbidden` is consulted before `allowed`, so a deny beats an allow
 * of the same specificity.
 */
internal object RuleMatcher {

    /**
     * Whether the debug stick may target the block at all.
     *
     * [tagIds] is a supplier because the block-level rules usually answer the question
     * on their own, and resolving a block's tags is not free.
     */
    fun isBlockAllowed(
        cfg: StickConfig,
        blockId: String,
        propertyNames: Collection<String>,
        tagIds: () -> List<String>,
    ): Boolean {
        // 1) exact block rules. A forbidden entry that names properties only narrows
        //    them down, so the block itself stays reachable.
        if (cfg.allowed.block(blockId) != null) return true
        cfg.forbidden.block(blockId)?.let { return !it.isEmpty }

        // 2) tag rules, same reading
        val tags = tagIds()
        if (tags.any { cfg.allowed.tag(it) != null }) return true
        for (tag in tags) {
            cfg.forbidden.tag(tag)?.let { return !it.isEmpty }
        }

        // 3) reachable if any of the block's properties is globally allowed
        if (propertyNames.any { cfg.allowed.properties.containsKey(it) }) return true

        // 4) unmentioned
        return !cfg.whitelist
    }

    /**
     * Whether [propertyName] may be selected. [blockId] is `null` when no block context
     * is available, which skips straight to the global lists.
     */
    fun isPropertyAllowed(
        cfg: StickConfig,
        propertyName: String,
        blockId: String?,
        tagIds: () -> List<String>,
    ): Boolean {
        if (blockId != null) {
            // 1) exact block rules
            cfg.forbidden.block(blockId)?.let {
                if (it.forbidsProperty(propertyName)) return false
            }
            cfg.allowed.block(blockId)?.let {
                if (it.allowsProperty(propertyName)) return true
                // The block is allowed but says nothing about properties: defer to the
                // global forbidden list rather than to the tags below.
                if (it.isEmpty) return !cfg.forbidden.globallyForbidsAll(propertyName)
            }

            // 2) tag rules
            val tags = tagIds()
            for (tag in tags) {
                cfg.forbidden.tag(tag)?.let {
                    if (it.forbidsProperty(propertyName)) return false
                }
            }
            for (tag in tags) {
                cfg.allowed.tag(tag)?.let {
                    if (it.allowsProperty(propertyName)) return true
                    if (it.isEmpty) return !cfg.forbidden.globallyForbidsAll(propertyName)
                }
            }
        }

        // 3) global property lists
        if (cfg.allowed.properties.containsKey(ALL) ||
            cfg.allowed.properties.containsKey(propertyName)
        ) {
            return true
        }
        cfg.forbidden.properties[propertyName]?.let { return !it.contains(ALL) }

        // 4) unmentioned
        return !cfg.whitelist
    }

    /** Whether [propertyName] may be cycled to [value] on this block. */
    fun isPropertyValueAllowed(
        cfg: StickConfig,
        blockId: String,
        propertyName: String,
        value: String,
        tagIds: () -> List<String>,
    ): Boolean {
        // 1) exact block rules
        cfg.forbidden.block(blockId)?.let {
            if (it.coversValue(propertyName, value)) return false
        }
        cfg.allowed.block(blockId)?.let {
            if (it.coversValue(propertyName, value)) return true
            if (it.isEmpty) return !cfg.forbidden.globallyForbidsAll(propertyName)
        }

        // 2) tag rules
        val tags = tagIds()
        for (tag in tags) {
            cfg.forbidden.tag(tag)?.let {
                if (it.coversValue(propertyName, value)) return false
            }
        }
        for (tag in tags) {
            cfg.allowed.tag(tag)?.let {
                if (it.coversValue(propertyName, value)) return true
                if (it.isEmpty) return !cfg.forbidden.globallyForbidsAll(propertyName)
            }
        }

        // 3) global property lists. `"all"` inside the value list counts here too, so
        //    that e.g. `forbidden.properties = {"waterlogged": ["all"]}` still bites
        //    with `whitelist = false` — upstream compared literal values only and let
        //    every waterlogged toggle through in blacklist mode.
        if (cfg.allowed.properties.containsKey(ALL)) return true
        cfg.allowed.properties[propertyName]?.let {
            if (it.contains(ALL) || it.contains(value)) return true
        }
        if (cfg.forbidden.properties.containsKey(ALL)) return false
        cfg.forbidden.properties[propertyName]?.let {
            if (it.contains(ALL) || it.contains(value)) return false
        }

        // 4) unmentioned
        return !cfg.whitelist
    }
}
