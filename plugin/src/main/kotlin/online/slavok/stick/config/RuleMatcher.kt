package online.slavok.stick.config

import online.slavok.stick.config.StickConfig.Companion.ALL

/**
 * The rule engine, expressed purely over ids and names — identical to the Fabric mod's
 * RuleMatcher, so the same config behaves the same on both. [Config] supplies the
 * block-id and tag lookups from the Bukkit side.
 *
 * Precedence, most specific first: exact block -> block tag -> global `properties` ->
 * `whitelist`. Within each level `forbidden` is consulted before `allowed`.
 */
internal object RuleMatcher {

    fun isBlockAllowed(
        cfg: StickConfig,
        blockId: String,
        propertyNames: Collection<String>,
        tagIds: () -> List<String>,
    ): Boolean {
        if (cfg.allowed.block(blockId) != null) return true
        cfg.forbidden.block(blockId)?.let { return !it.isEmpty }

        val tags = tagIds()
        if (tags.any { cfg.allowed.tag(it) != null }) return true
        for (tag in tags) {
            cfg.forbidden.tag(tag)?.let { return !it.isEmpty }
        }

        if (propertyNames.any { cfg.allowed.properties.containsKey(it) }) return true

        return !cfg.whitelist
    }

    fun isPropertyAllowed(
        cfg: StickConfig,
        propertyName: String,
        blockId: String?,
        tagIds: () -> List<String>,
    ): Boolean {
        if (blockId != null) {
            cfg.forbidden.block(blockId)?.let {
                if (it.forbidsProperty(propertyName)) return false
            }
            cfg.allowed.block(blockId)?.let {
                if (it.allowsProperty(propertyName)) return true
                if (it.isEmpty) return !cfg.forbidden.globallyForbidsAll(propertyName)
            }

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

        if (cfg.allowed.properties.containsKey(ALL) ||
            cfg.allowed.properties.containsKey(propertyName)
        ) {
            return true
        }
        cfg.forbidden.properties[propertyName]?.let { return !it.contains(ALL) }

        return !cfg.whitelist
    }

    fun isPropertyValueAllowed(
        cfg: StickConfig,
        blockId: String,
        propertyName: String,
        value: String,
        tagIds: () -> List<String>,
    ): Boolean {
        cfg.forbidden.block(blockId)?.let {
            if (it.coversValue(propertyName, value)) return false
        }
        cfg.allowed.block(blockId)?.let {
            if (it.coversValue(propertyName, value)) return true
            if (it.isEmpty) return !cfg.forbidden.globallyForbidsAll(propertyName)
        }

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

        if (cfg.allowed.properties.containsKey(ALL)) return true
        cfg.allowed.properties[propertyName]?.let {
            if (it.contains(ALL) || it.contains(value)) return true
        }
        if (cfg.forbidden.properties.containsKey(ALL)) return false
        cfg.forbidden.properties[propertyName]?.let {
            if (it.contains(ALL) || it.contains(value)) return false
        }

        return !cfg.whitelist
    }
}
