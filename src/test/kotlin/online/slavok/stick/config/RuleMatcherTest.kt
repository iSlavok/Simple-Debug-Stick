package online.slavok.stick.config

import online.slavok.stick.config.StickConfig.Companion.ALL
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Precedence rules of [RuleMatcher], exercised against the shipped defaults and a few
 * hand-built configs. Ids are passed literally, so no game bootstrap is needed.
 */
class RuleMatcherTest {

    private val defaults = StickConfig.default()

    // Tags a vanilla oak stair / oak slab actually carries, in registration order.
    private val stairTags = { listOf("minecraft:stairs", "minecraft:wooden_stairs") }
    private val slabTags = { listOf("minecraft:slabs", "minecraft:wooden_slabs") }
    private val noTags = { emptyList<String>() }

    private fun blockAllowed(
        cfg: StickConfig = defaults,
        id: String,
        properties: Collection<String> = emptyList(),
        tags: () -> List<String> = noTags,
    ) = RuleMatcher.isBlockAllowed(cfg, id, properties, tags)

    private fun propertyAllowed(
        cfg: StickConfig = defaults,
        id: String?,
        property: String,
        tags: () -> List<String> = noTags,
    ) = RuleMatcher.isPropertyAllowed(cfg, property, id, tags)

    private fun valueAllowed(
        cfg: StickConfig = defaults,
        id: String,
        property: String,
        value: String,
        tags: () -> List<String> = noTags,
    ) = RuleMatcher.isPropertyValueAllowed(cfg, id, property, value, tags)

    // --- block level ---------------------------------------------------------

    @Test
    fun `a tag rule reaches every block carrying the tag`() {
        assertTrue(blockAllowed(id = "minecraft:oak_stairs", tags = stairTags))
    }

    @Test
    fun `a block rule reaches the exact block, namespace optional in the config`() {
        assertTrue(blockAllowed(id = "minecraft:iron_bars"))
        assertTrue(blockAllowed(id = "minecraft:bamboo"))
    }

    @Test
    fun `whitelist mode denies anything no rule mentions`() {
        assertFalse(blockAllowed(id = "minecraft:repeater", properties = listOf("delay", "powered")))
    }

    @Test
    fun `blacklist mode permits anything no rule mentions`() {
        val cfg = defaults.copy(whitelist = false)
        assertTrue(blockAllowed(cfg, id = "minecraft:repeater", properties = listOf("delay")))
    }

    @Test
    fun `a forbidden entry naming properties leaves the block itself selectable`() {
        val cfg = defaults.copy(
            forbidden = StickConfig.Rules(
                blocks = listOf(StickConfig.Entry("oak_stairs", mapOf("shape" to listOf(ALL)))),
                properties = emptyMap(),
                tags = emptyList(),
            ),
        )
        assertTrue(blockAllowed(cfg, id = "minecraft:oak_stairs", tags = stairTags))
        assertFalse(propertyAllowed(cfg, id = "minecraft:oak_stairs", property = "shape", tags = stairTags))
        assertTrue(propertyAllowed(cfg, id = "minecraft:oak_stairs", property = "facing", tags = stairTags))
    }

    @Test
    fun `an empty forbidden entry removes the block entirely, siblings untouched`() {
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
    fun `a globally allowed property makes its blocks reachable`() {
        val cfg = defaults.copy(
            allowed = defaults.allowed.copy(properties = mapOf("facing" to listOf(ALL))),
        )
        assertTrue(blockAllowed(cfg, id = "minecraft:furnace", properties = listOf("facing", "lit")))
        assertFalse(blockAllowed(cfg, id = "minecraft:sand", properties = emptyList()))
    }

    // --- property level ------------------------------------------------------

    @Test
    fun `the global forbidden list beats a tag allow`() {
        assertTrue(blockAllowed(id = "minecraft:oak_stairs", tags = stairTags))
        assertFalse(propertyAllowed(id = "minecraft:oak_stairs", property = "waterlogged", tags = stairTags))
        assertFalse(
            valueAllowed(id = "minecraft:oak_stairs", property = "waterlogged", value = "true", tags = stairTags),
        )
    }

    @Test
    fun `a bare tag rule permits the properties of its blocks`() {
        assertTrue(propertyAllowed(id = "minecraft:oak_stairs", property = "facing", tags = stairTags))
        assertTrue(
            valueAllowed(id = "minecraft:oak_stairs", property = "facing", value = "north", tags = stairTags),
        )
    }

    @Test
    fun `a tag rule listing values permits only those values`() {
        // "slabs" is configured with type: [top, bottom] — "double" would duplicate the
        // slab out of thin air, so it must stay out of reach.
        assertTrue(valueAllowed(id = "minecraft:oak_slab", property = "type", value = "top", tags = slabTags))
        assertTrue(valueAllowed(id = "minecraft:oak_slab", property = "type", value = "bottom", tags = slabTags))
        assertFalse(valueAllowed(id = "minecraft:oak_slab", property = "type", value = "double", tags = slabTags))
    }

    @Test
    fun `a block rule listing values permits only those values`() {
        assertTrue(valueAllowed(id = "minecraft:bamboo", property = "leaves", value = "none"))
        assertTrue(valueAllowed(id = "minecraft:bamboo", property = "leaves", value = "large"))
        assertFalse(valueAllowed(id = "minecraft:bamboo", property = "leaves", value = "medium"))
        // "age": ["all"] — every value of that property is fair game.
        assertTrue(valueAllowed(id = "minecraft:bamboo", property = "age", value = "0"))
        assertTrue(valueAllowed(id = "minecraft:bamboo", property = "age", value = "1"))
        // "stage" is not listed at all.
        assertFalse(propertyAllowed(id = "minecraft:bamboo", property = "stage"))
    }

    @Test
    fun `an empty allow entry defers to the global forbidden list, not to whitelist`() {
        // iron_bars is allowed with no properties; waterlogged is globally forbidden.
        assertFalse(propertyAllowed(id = "minecraft:iron_bars", property = "waterlogged"))
        // ...but any other property of it is fine, even though whitelist is on.
        assertTrue(propertyAllowed(id = "minecraft:iron_bars", property = "north"))
    }

    @Test
    fun `a global forbidden all still bites in blacklist mode`() {
        // Upstream compared literal values only, so `["all"]` was ignored at the value
        // level and every block could be waterlogged as soon as whitelist was off.
        val cfg = defaults.copy(whitelist = false)
        assertFalse(valueAllowed(cfg, id = "minecraft:cobblestone_wall", property = "waterlogged", value = "true"))
    }

    @Test
    fun `an unknown property falls through to whitelist mode`() {
        assertFalse(propertyAllowed(id = "minecraft:repeater", property = "delay"))
        assertTrue(propertyAllowed(defaults.copy(whitelist = false), id = "minecraft:repeater", property = "delay"))
    }

    @Test
    fun `a null block context skips straight to the global lists`() {
        assertFalse(propertyAllowed(id = null, property = "waterlogged"))
        val cfg = defaults.copy(allowed = defaults.allowed.copy(properties = mapOf("facing" to listOf(ALL))))
        assertTrue(propertyAllowed(cfg, id = null, property = "facing"))
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
        assertTrue(propertyAllowed(cfg, id = "minecraft:iron_bars", property = "north"))
        assertFalse(propertyAllowed(cfg, id = "minecraft:iron_bars", property = "south"))
    }
}
