package online.slavok.stick

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * The permission gate: only a *creative* operator is handed to the vanilla debug stick;
 * a survival operator (and any player with `simpledebugstick.use`) gets the filtered
 * behaviour — the bug where a survival op could edit nothing at all.
 */
class DebugStickListenerTest {

    private fun interactEvent(gameMode: GameMode, bypass: Boolean, use: Boolean): Pair<PlayerInteractEvent, Player> {
        val item = mock<ItemStack> { on { type } doReturn Material.DEBUG_STICK }
        val block = mock<Block>()
        val player = mock<Player> {
            on { this.gameMode } doReturn gameMode
            on { hasPermission("simpledebugstick.bypass") } doReturn bypass
            on { hasPermission("simpledebugstick.use") } doReturn use
            on { isSneaking } doReturn false
        }
        val event = mock<PlayerInteractEvent> {
            on { this.item } doReturn item
            on { action } doReturn Action.RIGHT_CLICK_BLOCK
            on { this.player } doReturn player
            on { clickedBlock } doReturn block
        }
        return event to player
    }

    @Test
    fun `survival operator is filtered, not bypassed`() {
        val handler = mock<DebugStickHandler>()
        val (event, player) = interactEvent(GameMode.SURVIVAL, bypass = true, use = true)
        DebugStickListener(handler).onInteract(event)
        verify(handler).handle(eq(player), any(), eq(true), eq(false))
        verify(event).isCancelled = true
    }

    @Test
    fun `creative operator bypasses to vanilla`() {
        val handler = mock<DebugStickHandler>()
        val (event, _) = interactEvent(GameMode.CREATIVE, bypass = true, use = true)
        DebugStickListener(handler).onInteract(event)
        verify(handler, never()).handle(any(), any(), any(), any())
        verify(event, never()).isCancelled = true
    }

    @Test
    fun `survival player with use permission is filtered`() {
        val handler = mock<DebugStickHandler>()
        val (event, player) = interactEvent(GameMode.SURVIVAL, bypass = false, use = true)
        DebugStickListener(handler).onInteract(event)
        verify(handler).handle(eq(player), any(), eq(true), eq(false))
    }

    @Test
    fun `player without use permission is ignored`() {
        val handler = mock<DebugStickHandler>()
        val (event, _) = interactEvent(GameMode.SURVIVAL, bypass = false, use = false)
        DebugStickListener(handler).onInteract(event)
        verify(handler, never()).handle(any(), any(), any(), any())
        verify(event, never()).isCancelled = true
    }
}
