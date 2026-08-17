package online.slavok.stick.config

import online.slavok.stick.config.StickConfig.Companion.ALL
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The plugin's rule engine must behave identically to the mod's, so the same
 * `SimpleDebugStick.json` gives the same result on a server and a client. These mirror the
 * mod's RuleMatcher tests; ids are passed literally, so no server is needed.
 */
class RuleMatcherTest {

    // A fixed decorative allow-list fixture, independent of the shipped default() (which
    // may change) — the same config the mod's RuleMatcher tests use.
    private val defaults = StickConfig(
        allowed = StickConfig.Rules(
            blocks = listOf(
                StickConfig.Entry("iron_bars"),
                StickConfig.Entry("bamboo", mapOf("leaves" to listOf("none", "large"), "age" to listOf(ALL))),
            ),
            properties = emptyMap(),
            tags = listOf(
                StickConfig.Entry("stairs"),
                StickConfig.Entry("walls"),
                StickConfig.Entry("c:glass_panes"),
                StickConfig.Entry("fences"),
                StickConfig.Entry("slabs", mapOf("type" to listOf("top", "bottom"))),
            ),
        ),
        forbidden = StickConfig.Rules(emptyList(), mapOf("waterlogged" to listOf(ALL)), emptyList()),
        whitelist = true,
    )
    private val stairTags = { listOf("minecraft:stairs", "minecraft:wooden_stairs") }
    private val slabTags = { listOf("minecraft:slabs", "minecraft:wooden_slabs") }
    private val noTags = { emptyList<String>() }

    private fun blockAllowed(cfg: StickConfig = defaults, id: String, props: Collection<String> = emptyList(), tags: () -> List<String> = noTags) =
        RuleMatcher.isBlockAllowed(cfg, id, props, tags)

    private fun propAllowed(cfg: StickConfig = defaults, id: String?, prop: String, tags: () -> List<String> = noTags) =
        RuleMatcher.isPropertyAllowed(cfg, prop, id, tags)

    private fun valueAllowed(cfg: StickConfig = defaults, id: String, prop: String, value: String, tags: () -> List<String> = noTags) =
        RuleMatcher.isPropertyValueAllowed(cfg, id, prop, value, tags)

    @Test
    fun `tag rule reaches every block with the tag`() {
        assertTrue(blockAllowed(id = "minecraft:oak_stairs", tags = stairTags))
    }

    @Test
    fun `block rule reaches the exact block, namespace optional`() {
        assertTrue(blockAllowed(id = "minecraft:iron_bars"))
        assertTrue(blockAllowed(id = "minecraft:bamboo"))
    }

    @Test
    fun `whitelist denies the unmentioned, blacklist permits it`() {
        assertFalse(blockAllowed(id = "minecraft:repeater", props = listOf("delay")))
        assertTrue(blockAllowed(defaults.copy(whitelist = false), id = "minecraft:repeater", props = listOf("delay")))
    }

    @Test
    fun `global forbidden beats a tag allow`() {
        assertTrue(blockAllowed(id = "minecraft:oak_stairs", tags = stairTags))
        assertFalse(propAllowed(id = "minecraft:oak_stairs", prop = "waterlogged", tags = stairTags))
        assertFalse(valueAllowed(id = "minecraft:oak_stairs", prop = "waterlogged", value = "true", tags = stairTags))
    }

    @Test
    fun `a tag rule listing values permits only those values`() {
        assertTrue(valueAllowed(id = "minecraft:oak_slab", prop = "type", value = "top", tags = slabTags))
        assertTrue(valueAllowed(id = "minecraft:oak_slab", prop = "type", value = "bottom", tags = slabTags))
        assertFalse(valueAllowed(id = "minecraft:oak_slab", prop = "type", value = "double", tags = slabTags))
    }

    @Test
    fun `a block rule listing values permits only those values`() {
        assertTrue(valueAllowed(id = "minecraft:bamboo", prop = "leaves", value = "none"))
        assertTrue(valueAllowed(id = "minecraft:bamboo", prop = "leaves", value = "large"))
        assertFalse(valueAllowed(id = "minecraft:bamboo", prop = "leaves", value = "medium"))
        assertTrue(valueAllowed(id = "minecraft:bamboo", prop = "age", value = "1"))
        assertFalse(propAllowed(id = "minecraft:bamboo", prop = "stage"))
    }

    @Test
    fun `a global forbidden all still bites in blacklist mode`() {
        val cfg = defaults.copy(whitelist = false)
        assertFalse(valueAllowed(cfg, id = "minecraft:cobblestone_wall", prop = "waterlogged", value = "true"))
    }

    @Test
    fun `an empty forbidden entry removes the block, siblings untouched`() {
        val cfg = defaults.copy(
            forbidden = StickConfig.Rules(
                blocks = listOf(StickConfig.Entry("oak_stairs")),
                properties = emptyMap(),
                tags = emptyList(),
            ),
        )
        assertFalse(blockAllowed(cfg, id = "minecraft:oak_stairs", tags = stairTags))
        assertTrue(blockAllowed(cfg, id = "minecraft:stone_stairs", tags = stairTags))
    }

    @Test
    fun `duplicate ids keep the first entry`() {
        val cfg = defaults.copy(
            allowed = defaults.allowed.copy(
                blocks = listOf(
                    StickConfig.Entry("iron_bars", mapOf("north" to listOf(ALL))),
                    StickConfig.Entry("iron_bars", mapOf("south" to listOf(ALL))),
                ),
            ),
        )
        assertTrue(propAllowed(cfg, id = "minecraft:iron_bars", prop = "north"))
        assertFalse(propAllowed(cfg, id = "minecraft:iron_bars", prop = "south"))
    }
}
