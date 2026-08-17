package online.slavok.stick

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock

/** Smoke test: the plugin enables cleanly and registers its command + listener. */
class PluginLoadTest {

    private lateinit var server: ServerMock
    private lateinit var plugin: SimpleDebugStickPlugin

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(SimpleDebugStickPlugin::class.java)
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `plugin enables`() {
        assertTrue(plugin.isEnabled)
    }

    @Test
    fun `command is registered`() {
        assertNotNull(server.getPluginCommand("simpledebugstick"))
    }
}
