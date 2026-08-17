package online.slavok.stick

import online.slavok.stick.config.ConfigManager
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** `/simpledebugstick give` hands out a debug stick; `reload` re-reads the config. */
class DebugStickCommand(private val config: ConfigManager) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        when (args.firstOrNull()?.lowercase()) {
            "give" -> {
                val target = (args.getOrNull(1)?.let { sender.server.getPlayerExact(it) }) ?: sender as? Player
                if (target == null) {
                    sender.sendMessage("Usage: /$label give <player>")
                    return true
                }
                target.inventory.addItem(ItemStack(Material.DEBUG_STICK))
                sender.sendMessage("Gave a debug stick to ${target.name}.")
            }

            "reload" -> {
                config.load()
                sender.sendMessage("Simple Debug Stick config reloaded.")
            }

            else -> sender.sendMessage("Usage: /$label <give|reload>")
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): List<String> = when (args.size) {
        1 -> listOf("give", "reload").filter { it.startsWith(args[0].lowercase()) }
        2 -> if (args[0].equals("give", true)) {
            sender.server.onlinePlayers.map { it.name }.filter { it.startsWith(args[1], true) }
        } else emptyList()
        else -> emptyList()
    }
}
