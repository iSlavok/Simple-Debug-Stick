package online.slavok.stick

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.item.Items
import net.minecraft.util.ActionResult
import online.slavok.stick.config.Config
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

object SimpleDebugStick : ModInitializer {
    const val MOD_ID = "simple-debug-stick"

    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    /**
     * Vanilla ties the debug stick's "select next property" action to *breaking* a
     * block, which a survival player never manages in a single click. We re-trigger it
     * from a punch instead — but [AttackBlockCallback] fires every tick while the
     * button is held, so an unguarded punch would cycle properties 20 times a second.
     */
    private const val ATTACK_COOLDOWN_TICKS = 5L

    /**
     * World tick of the last accepted punch, per player. Upstream stamped this onto the
     * stack's `custom_data` component, which stopped debug sticks from stacking and
     * re-synced the item to the client several times a second. Only ever touched from
     * the server thread — the callback below returns early on the client.
     */
    private val lastAttackTick = HashMap<UUID, Long>()

    override fun onInitialize() {
        Config.loadOrCreate()

        AttackBlockCallback.EVENT.register { player, world, hand, pos, _ ->
            // The callback runs before vanilla's spectator check, and creative players
            // already get the real debug stick, so neither needs survival handling.
            if (world.isClient || player.isSpectator || player.isCreative) {
                return@register ActionResult.PASS
            }

            val stack = player.getStackInHand(hand)
            if (!stack.isOf(Items.DEBUG_STICK)) return@register ActionResult.PASS

            val now = world.time
            val last = lastAttackTick[player.uuid]
            if (last != null && now < last + ATTACK_COOLDOWN_TICKS) {
                return@register ActionResult.PASS
            }
            lastAttackTick[player.uuid] = now

            // DebugStickItem.canMine runs the property-cycling logic (server side only);
            // DebugStickMixin filters what it is allowed to touch through the config.
            stack.item.canMine(stack, world.getBlockState(pos), world, pos, player)
            ActionResult.PASS
        }

        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            lastAttackTick.remove(handler.player.uuid)
        }

        LOGGER.info("Simple Debug Stick loaded successfully!")
    }
}
