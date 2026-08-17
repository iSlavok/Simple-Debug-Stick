package online.slavok.stick.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import online.slavok.stick.state.StateProperties
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Tag
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

/**
 * Loads/saves the shared `SimpleDebugStick.json` and answers the debug stick's questions
 * about it, resolving Bukkit [Material]s and block tags for the pure [RuleMatcher].
 */
class ConfigManager(private val file: File, private val logger: Logger) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    @Volatile
    var config: StickConfig = StickConfig.default()
        private set

    // Resolving a material's tags scans the whole block-tag registry, so memoize it.
    private val tagCache = ConcurrentHashMap<Material, List<String>>()

    fun loadOrCreate() {
        if (file.exists()) load() else save()
    }

    /** Re-reads the file; a malformed file is reported and the current settings kept. */
    fun load() {
        if (!file.exists()) {
            save()
            return
        }
        try {
            val json = file.bufferedReader().use { JsonParser.parseReader(it) }
            config = StickConfig.fromJson(gson, json)
            tagCache.clear()
            logger.info("Loaded ${file.name}")
        } catch (e: Exception) {
            logger.warning("Could not read ${file.name}, keeping current settings: ${e.message}")
        }
    }

    fun save() {
        try {
            file.parentFile?.mkdirs()
            file.bufferedWriter().use { gson.toJson(config, it) }
            logger.info("Wrote ${file.name}")
        } catch (e: Exception) {
            logger.warning("Could not write ${file.name}: ${e.message}")
        }
    }

    // -- queries ---------------------------------------------------------------

    fun isBlockAllowed(material: Material): Boolean = RuleMatcher.isBlockAllowed(
        cfg = config,
        blockId = blockId(material),
        propertyNames = propertyNames(material),
        tagIds = { tagIds(material) },
    )

    fun isPropertyAllowed(propertyName: String, material: Material?): Boolean =
        RuleMatcher.isPropertyAllowed(
            cfg = config,
            propertyName = propertyName,
            blockId = material?.let(::blockId),
            tagIds = { material?.let(::tagIds).orEmpty() },
        )

    fun isPropertyValueAllowed(material: Material, propertyName: String, value: String): Boolean =
        RuleMatcher.isPropertyValueAllowed(
            cfg = config,
            blockId = blockId(material),
            propertyName = propertyName,
            value = value,
            tagIds = { tagIds(material) },
        )

    // -- Bukkit resolvers ------------------------------------------------------

    private fun blockId(material: Material): String = material.key.toString()

    private fun propertyNames(material: Material): List<String> =
        if (material.isBlock) StateProperties.of(material.createBlockData()).map { it.name } else emptyList()

    private fun tagIds(material: Material): List<String> = tagCache.getOrPut(material) {
        Bukkit.getTags(Tag.REGISTRY_BLOCKS, Material::class.java)
            .filter { it.isTagged(material) }
            .map { it.key.toString() }
    }
}
