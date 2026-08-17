package online.slavok.stick

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
//? if >=1.22 {
/*import net.minecraft.world.item.Items
import net.minecraft.world.InteractionResult
*///?} else {
import net.minecraft.item.Items
import net.minecraft.util.ActionResult
//?}
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
            //? if >=1.22 {
            /*val pass = InteractionResult.PASS
            *///?} else {
            val pass = ActionResult.PASS
            //?}

            // The callback runs before vanilla's spectator check, and creative players
            // already get the real debug stick, so neither needs survival handling.
            //? if >=1.22 {
            /*val onClient = world.isClientSide
            *///?} else {
            val onClient = world.isClient
            //?}
            if (onClient || player.isSpectator || player.isCreative) {
                return@register pass
            }

            //? if >=1.22 {
            /*val stack = player.getItemInHand(hand)
            if (!stack.`is`(Items.DEBUG_STICK)) return@register pass
            val now = world.gameTime
            val pid = player.getUUID()
            *///?} else {
            val stack = player.getStackInHand(hand)
            if (!stack.isOf(Items.DEBUG_STICK)) return@register pass
            val now = world.time
            val pid = player.uuid
            //?}

            val last = lastAttackTick[pid]
            if (last != null && now < last + ATTACK_COOLDOWN_TICKS) {
                return@register pass
            }
            lastAttackTick[pid] = now

            // The debug stick's property-cycling logic runs server-side inside canMine /
            // canDestroyBlock; DebugStickMixin filters what it may touch via the config.
            // The ItemStack parameter was added in 1.21.2; the method was renamed to
            // canDestroyBlock in 26.
            //? if >=1.22 {
            /*stack.item.canDestroyBlock(stack, world.getBlockState(pos), world, pos, player)
            *///?} elif >=1.21.2 {
            stack.item.canMine(stack, world.getBlockState(pos), world, pos, player)
            //?} else {
            /*stack.item.canMine(world.getBlockState(pos), world, pos, player)
            *///?}
            pass
        }

        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            //? if >=1.22 {
            /*lastAttackTick.remove(handler.player.getUUID())
            *///?} else {
            lastAttackTick.remove(handler.player.uuid)
            //?}
        }

        LOGGER.info("Simple Debug Stick loaded successfully!")
    }
}
