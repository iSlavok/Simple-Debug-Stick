package online.slavok.stick

import online.slavok.stick.config.ConfigManager
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

/**
 * Server-plugin port of the Simple Debug Stick mod: gives survival players the debug stick,
 * filtered to the block states the operator allows, on Bukkit/Spigot/Paper/Purpur/Folia.
 *
 * Non-final so MockBukkit can subclass it in tests.
 */
open class SimpleDebugStickPlugin : JavaPlugin() {

    override fun onEnable() {
        val config = ConfigManager(File(dataFolder, "SimpleDebugStick.json"), logger)
        config.loadOrCreate()

        val handler = DebugStickHandler(config)
        server.pluginManager.registerEvents(DebugStickListener(handler), this)

        getCommand("simpledebugstick")?.let {
            val command = DebugStickCommand(config)
            it.setExecutor(command)
            it.tabCompleter = command
        }

        // Smithing recipes are a 1.20+ Bukkit API; on older servers the stick is
        // command-only. Isolated so the missing class never aborts enable.
        try {
            DebugStickRecipe.register(this)
        } catch (t: Throwable) {
            logger.info("Smithing recipe not registered (needs a 1.20+ server): ${t.message}")
        }

        logger.info("Simple Debug Stick enabled.")
    }
}
