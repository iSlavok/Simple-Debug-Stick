package online.slavok.stick.config

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StickConfigTest {

    private val gson = Gson()

    private fun parse(json: String): StickConfig =
        StickConfig.fromJson(gson, JsonParser.parseString(json))

    @Test
    fun `parses the full schema`() {
        val cfg = parse(
            """
            {
              "allowed": {
                "blocks": [ { "id": "iron_bars" },
                            { "id": "bamboo", "properties": { "leaves": ["none","large"] } } ],
                "properties": {},
                "tags": [ { "id": "stairs" }, { "id": "slabs", "properties": { "type": ["top","bottom"] } } ]
              },
              "forbidden": { "blocks": [], "properties": { "waterlogged": ["all"] }, "tags": [] },
              "whitelist": true
            }
            """.trimIndent(),
        )
        assertTrue(cfg.whitelist)
        assertEquals("minecraft:iron_bars", cfg.allowed.block("minecraft:iron_bars")?.normalizedId)
        assertTrue(cfg.allowed.tag("minecraft:stairs")!!.isEmpty)
        assertEquals(listOf("top", "bottom"), cfg.allowed.tag("minecraft:slabs")!!.properties["type"])
        assertTrue(cfg.forbidden.globallyForbidsAll("waterlogged"))
    }

    @Test
    fun `missing whitelist defaults to true and missing sections to empty`() {
        val cfg = parse("""{ "allowed": { "blocks": [ { "id": "stone" } ] } }""")
        assertTrue(cfg.whitelist)
        assertTrue(cfg.forbidden.blocks.isEmpty())
        assertTrue(cfg.allowed.tags.isEmpty())
        assertTrue(cfg.allowed.block("minecraft:stone")!!.isEmpty)
    }

    @Test
    fun `default config round-trips through Gson`() {
        val original = StickConfig.default()
        val restored = StickConfig.fromJson(gson, JsonParser.parseString(gson.toJson(original)))
        assertEquals(original, restored)
    }

    @Test
    fun `serialized file has no derived fields (matches the mod's schema)`() {
        val json = gson.toJson(StickConfig.default())
        assertFalse(json.contains("normalizedId"), "on-disk config must not leak derived fields")
        assertFalse(json.contains("blockIndex"))
        assertFalse(json.contains("isEmpty"))
    }
}
