package online.slavok.stick

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.SmithingTransformRecipe
import org.bukkit.plugin.Plugin

/**
 * Registers the smithing-table recipe (netherite upgrade template + stick + netherite
 * ingot -> debug stick). Isolated in its own class so the SmithingTransformRecipe
 * reference only loads when [register] is called — on servers older than 1.20 the caller
 * catches the resulting error and skips the recipe (the stick is still available via the
 * command).
 */
object DebugStickRecipe {
    fun register(plugin: Plugin) {
        val key = NamespacedKey(plugin, "debug_stick_smithing")
        Bukkit.removeRecipe(key) // tolerate a re-enable
        Bukkit.addRecipe(
            SmithingTransformRecipe(
                key,
                ItemStack(Material.DEBUG_STICK),
                RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                RecipeChoice.MaterialChoice(Material.STICK),
                RecipeChoice.MaterialChoice(Material.NETHERITE_INGOT),
            ),
        )
    }
}
