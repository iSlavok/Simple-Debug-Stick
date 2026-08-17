package online.slavok.stick

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Routes debug-stick clicks to [DebugStickHandler]. Right-click a block changes the value,
 * left-click selects the property, sneaking reverses — the same UX as the mod's
 * punch-to-select. Left-clicks are rate-limited per player so holding the button does not
 * spin the property list.
 *
 * Mirrors the mod's `isCreativeLevelTwoOp` exemption: only a **creative-mode** holder of
 * `simpledebugstick.bypass` is left to the unrestricted vanilla debug stick (vanilla only
 * works in creative anyway). A survival operator still gets the filtered stick — otherwise
 * the debug stick would do nothing at all for them. Players without `simpledebugstick.use`
 * are ignored entirely.
 */
class DebugStickListener(private val handler: DebugStickHandler) : Listener {

    private val lastLeftClick = ConcurrentHashMap<UUID, Long>()

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEvent) {
        val item = event.item ?: return
        if (item.type != Material.DEBUG_STICK) return

        val change = when (event.action) {
            Action.RIGHT_CLICK_BLOCK -> true
            Action.LEFT_CLICK_BLOCK -> false
            else -> return
        }

        val player = event.player
        // Only a creative operator keeps the unrestricted vanilla stick; a survival op is
        // filtered like everyone else (vanilla's debug stick does nothing in survival).
        if (player.gameMode == GameMode.CREATIVE && player.hasPermission("simpledebugstick.bypass")) return
        if (!player.hasPermission("simpledebugstick.use")) return

        // Our handling replaces vanilla's (and stops the left-click from breaking the block).
        event.isCancelled = true

        val block = event.clickedBlock ?: return

        if (!change) {
            // Holding left-click re-fires; debounce to one selection per quarter second.
            val now = System.currentTimeMillis()
            val last = lastLeftClick[player.uniqueId]
            if (last != null && now - last < 250L) return
            lastLeftClick[player.uniqueId] = now
        }

        handler.handle(player, block, change, player.isSneaking)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        handler.forget(event.player.uniqueId)
        lastLeftClick.remove(event.player.uniqueId)
    }
}
