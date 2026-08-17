package online.slavok.stick

import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TranslatableComponent
import online.slavok.stick.config.ConfigManager
import online.slavok.stick.state.StateProperties
import online.slavok.stick.state.StateProperty
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * The debug stick's cycling logic, filtered through the config — the plugin analogue of
 * the mod's DebugStickMixin. Right-click changes the selected property's value; left-click
 * selects the next property; sneaking reverses. Only properties and values the config
 * permits are ever reached, so a survival player edits exactly what the operator allowed.
 *
 * The selected property is remembered per player and block type (mirroring the vanilla
 * debug-stick component), in memory, and dropped when the player quits.
 */
class DebugStickHandler(private val config: ConfigManager) {

    private val selected = ConcurrentHashMap<UUID, MutableMap<Material, String>>()

    fun forget(uuid: UUID) {
        selected.remove(uuid)
    }

    fun handle(player: Player, block: Block, changeValue: Boolean, inverse: Boolean) {
        val material = block.type
        val data = block.blockData.clone()
        val properties = StateProperties.of(data).filter { config.isPropertyAllowed(it.name, material) }

        if (!config.isBlockAllowed(material) || properties.isEmpty()) {
            deny(player, material)
            return
        }

        val perBlock = selected.getOrPut(player.uniqueId) { HashMap() }

        if (changeValue) {
            val property = properties.firstOrNull { it.name == perBlock[material] } ?: properties.first()
            val value = nextAllowedValue(material, property, inverse) ?: return
            property.set(value)
            block.blockData = data // region-local on Folia: the clicked block is in the caller's region
            actionBar(player, "item.minecraft.debug_stick.update", property.name, value)
        } else {
            val names = properties.map { it.name }
            val current = names.indexOf(perBlock[material])
            val next = if (current < 0) {
                if (inverse) properties.size - 1 else 0
            } else {
                Math.floorMod(current + if (inverse) -1 else 1, properties.size)
            }
            val property = properties[next]
            perBlock[material] = property.name
            actionBar(player, "item.minecraft.debug_stick.select", property.name, property.current())
        }
    }

    /** Next value of [property] the config permits, cycling from the current one; null if none. */
    private fun nextAllowedValue(material: Material, property: StateProperty, inverse: Boolean): String? {
        val values = property.values
        val n = values.size
        val current = values.indexOf(property.current())
        for (step in 1..n) {
            val value = values[Math.floorMod(current + step * if (inverse) -1 else 1, n)]
            if (config.isPropertyValueAllowed(material, property.name, value)) return value
        }
        return null
    }

    private fun deny(player: Player, material: Material) {
        actionBar(player, "item.minecraft.debug_stick.empty", material.key.toString())
    }

    /**
     * Sends the vanilla debug-stick message to the action bar, using the vanilla
     * translation keys so the client renders it exactly like the real item (localized,
     * above the hotbar — no chat spam). This is the plugin's stand-in for the mod's reuse
     * of the item's own sendMessage, which needs NMS a plugin does not have.
     */
    private fun actionBar(player: Player, key: String, vararg args: String) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TranslatableComponent(key, *args))
    }
}
