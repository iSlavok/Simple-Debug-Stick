package online.slavok.stick.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.mojang.serialization.DataResult
import com.mojang.serialization.JsonOps
import net.fabricmc.loader.api.FabricLoader
//? if >=1.22 {
/*import net.minecraft.world.level.block.Block
import net.minecraft.core.registries.BuiltInRegistries
*///?} elif >=1.19 {
import net.minecraft.block.Block
import net.minecraft.registry.Registries
//?} else {
/*import net.minecraft.block.Block
import net.minecraft.util.registry.Registry
*///?}
import online.slavok.stick.SimpleDebugStick.LOGGER
import java.nio.file.Files
import java.nio.file.Path

/**
 * Holds the active [StickConfig] and adapts the game's registry to [RuleMatcher], which
 * owns the actual precedence rules.
 */
object Config {
    private val GSON = GsonBuilder().setPrettyPrinting().create()
    private val configFile: Path =
        FabricLoader.getInstance().configDir.resolve("SimpleDebugStick.json")

    @Volatile
    private var config: StickConfig = StickConfig.default()

    // -------------------------------------------------------------------------
    // Loading / saving
    // -------------------------------------------------------------------------

    /** Reads the config file, writing the defaults out first if it does not exist yet. */
    @JvmStatic
    fun loadOrCreate() {
        if (Files.exists(configFile)) load() else save()
    }

    /**
     * Re-reads the config file. A malformed file is reported and *kept* — the previous
     * (or default) settings stay active. Upstream overwrote it with the defaults here,
     * which silently threw away a hand-edited config over a single typo.
     */
    @JvmStatic
    fun load() {
        if (!Files.exists(configFile)) {
            save()
            return
        }
        try {
            val json = Files.newBufferedReader(configFile).use { JsonParser.parseReader(it) }
            config = StickConfig.CODEC.parse(JsonOps.INSTANCE, json).orThrow()
            LOGGER.info("Loaded {}", configFile)
        } catch (e: Exception) {
            LOGGER.error("Could not read {} — keeping the current settings.", configFile, e)
        }
    }

    /** Writes the active config, creating `config/` if needed. */
    @JvmStatic
    fun save() {
        try {
            configFile.parent?.let(Files::createDirectories)
            Files.newBufferedWriter(configFile).use { writer ->
                GSON.newJsonWriter(writer).use { json ->
                    json.setIndent("\t")
                    GSON.toJson(serialize(), json)
                }
            }
            LOGGER.info("Wrote {}", configFile)
        } catch (e: Exception) {
            LOGGER.error("Could not write {}", configFile, e)
        }
    }

    private fun serialize(): JsonElement =
        StickConfig.CODEC.encodeStart(JsonOps.INSTANCE, config).orThrow()

    // DataResult.getOrThrow() is no-arg since 1.20.5 (DataFixerUpper 8); before that
    // it took (allowPartial, onError).
    private fun <T> DataResult<T>.orThrow(): T =
        //? if >=1.20.5 {
        getOrThrow()
        //?} else {
        /*getOrThrow(false) {}*/
        //?}

    // -------------------------------------------------------------------------
    // Queries — called from DebugStickMixin
    // -------------------------------------------------------------------------

    /** Whether the debug stick may target [block] at all. */
    @JvmStatic
    fun isBlockAllowed(block: Block): Boolean = RuleMatcher.isBlockAllowed(
        cfg = config,
        blockId = idOf(block),
        //? if >=1.22 {
        /*propertyNames = block.stateDefinition.properties.map { it.name },
        *///?} else {
        propertyNames = block.stateManager.properties.map { it.name },
        //?}
        tagIds = { tagIdsOf(block) },
    )

    /**
     * Whether the property named [propertyName] may be selected, in the context of
     * [block] when one is known.
     */
    @JvmStatic
    fun isPropertyAllowed(propertyName: String, block: Block?): Boolean =
        RuleMatcher.isPropertyAllowed(
            cfg = config,
            propertyName = propertyName,
            blockId = block?.let(::idOf),
            tagIds = { block?.let(::tagIdsOf).orEmpty() },
        )

    /** The block's registry id as a namespaced string, e.g. `minecraft:oak_stairs`. */
    @JvmStatic
    fun blockId(block: Block): String = idOf(block)

    /** Whether [propertyName] of [block] may be cycled to [value]. */
    @JvmStatic
    fun isPropertyValueAllowed(block: Block, propertyName: String, value: String): Boolean =
        RuleMatcher.isPropertyValueAllowed(
            cfg = config,
            blockId = idOf(block),
            propertyName = propertyName,
            value = value,
            tagIds = { tagIdsOf(block) },
        )

    // -------------------------------------------------------------------------

    // The registry holder moved from net.minecraft.util.registry.Registry to
    // net.minecraft.registry.Registries in 1.19.3, and the value-keyed getEntry(T)
    // overload (used for streamTags) only exists on the newer holder.
    private fun idOf(block: Block): String =
        //? if >=1.22 {
        /*BuiltInRegistries.BLOCK.getKey(block).toString()
        *///?} elif >=1.19 {
        Registries.BLOCK.getId(block).toString()
        //?} else {
        /*Registry.BLOCK.getId(block).toString()
        *///?}

    private fun tagIdsOf(block: Block): List<String> =
        //? if >=1.22 {
        /*block.builtInRegistryHolder().tags().toList().map { it.location().toString() }
        *///?} elif >=1.19 {
        Registries.BLOCK.getEntry(block).streamTags().toList().map { it.id.toString() }
        //?} else {
        /*Registry.BLOCK.getKey(block)
            .flatMap { Registry.BLOCK.getEntry(it) }
            .map { entry -> entry.streamTags().map { it.id.toString() }.toList() }
            .orElse(emptyList())
        *///?}
}
