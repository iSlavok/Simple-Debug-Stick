package online.slavok.stick.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DebugStickStateComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DebugStickItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import online.slavok.stick.config.Config;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/**
 * Vanilla only lets a player edit block states once past the {@code isCreativeLevelTwoOp}
 * gate. This replaces the run for everyone below that gate with a config-filtered
 * version: the same UX, restricted to the blocks, properties and values the server
 * operator permits.
 */
@Mixin(DebugStickItem.class)
public abstract class DebugStickMixin extends Item {
    public DebugStickMixin(Settings settings) {
        super(settings);
    }

    @Shadow
    private static void sendMessage(PlayerEntity player, Text message) {}

    @Shadow
    private static <T extends Comparable<T>> String getValueString(BlockState state, Property<T> property) {
        return null;
    }

    @Shadow
    private static <T> T cycle(Iterable<T> values, @Nullable T value, boolean inverse) {
        return null;
    }

    @Inject(at = @At("HEAD"), method = "use", cancellable = true)
    private void simpleDebugStick$filterUse(
            PlayerEntity player,
            BlockState state,
            WorldAccess world,
            BlockPos pos,
            boolean update,
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        // Players who may already use the debug stick keep the unrestricted vanilla one.
        if (player.isCreativeLevelTwoOp()) {
            return;
        }

        Block block = state.getBlock();
        RegistryEntry<Block> registryEntry = state.getRegistryEntry();
        Collection<Property<?>> properties = registryEntry.value().getStateManager().getProperties();

        if (!Config.isBlockAllowed(block) || properties.isEmpty()) {
            simpleDebugStick$deny(player, registryEntry, cir);
            return;
        }

        // Vanilla remembers the selected property per block in a stack component
        // (https://minecraft.wiki/w/Debug_Stick). It is always present on a real debug
        // stick; if something stripped it, deny rather than fall through to the
        // unfiltered vanilla path.
        DebugStickStateComponent stateComponent = stack.get(DataComponentTypes.DEBUG_STICK_STATE);
        if (stateComponent == null) {
            simpleDebugStick$deny(player, registryEntry, cir);
            return;
        }

        Property<?> property = stateComponent.properties().get(registryEntry);

        if (update) {
            if (property == null) {
                property = simpleDebugStick$nextAllowedProperty(properties, null, block, false);
            }
            if (!Config.isPropertyAllowed(property.getName(), block)) {
                simpleDebugStick$deny(player, registryEntry, cir);
                return;
            }

            BlockState newState =
                    simpleDebugStick$nextAllowedState(state, property, player.shouldCancelInteraction());
            world.setBlockState(pos, newState, 18);
            sendMessage(player, Text.translatable(
                    this.getTranslationKey() + ".update",
                    property.getName(),
                    getValueString(newState, property)
            ));
        } else {
            property = simpleDebugStick$nextAllowedProperty(
                    properties, property, block, player.shouldCancelInteraction());
            if (!Config.isPropertyAllowed(property.getName(), block)) {
                simpleDebugStick$deny(player, registryEntry, cir);
                return;
            }
            stack.set(DataComponentTypes.DEBUG_STICK_STATE, stateComponent.with(registryEntry, property));
            sendMessage(player, Text.translatable(
                    this.getTranslationKey() + ".select",
                    property.getName(),
                    getValueString(state, property)
            ));
        }
        cir.setReturnValue(true);
    }

    /** Reports the block as having nothing to edit and swallows the vanilla call. */
    @Unique
    private void simpleDebugStick$deny(
            PlayerEntity player,
            RegistryEntry<Block> registryEntry,
            CallbackInfoReturnable<Boolean> cir
    ) {
        sendMessage(player, Text.translatable(
                this.getTranslationKey() + ".empty", registryEntry.getIdAsString()));
        cir.setReturnValue(false);
    }

    /**
     * Advances to the next property the config permits. Falls back to the first property
     * reached when none qualify — the caller re-checks and denies.
     */
    @Unique
    private Property<?> simpleDebugStick$nextAllowedProperty(
            Collection<Property<?>> properties,
            @Nullable Property<?> property,
            @Nullable Block block,
            boolean inverse
    ) {
        int i = 0;
        do {
            property = cycle(properties, property, inverse);
            i++;
        } while (i < properties.size() && !Config.isPropertyAllowed(property.getName(), block));
        return property;
    }

    /**
     * Advances {@code property} to its next permitted value, leaving the state untouched
     * when the config permits none of them.
     */
    @Unique
    private <T extends Comparable<T>> BlockState simpleDebugStick$nextAllowedState(
            BlockState state,
            Property<T> property,
            boolean inverse
    ) {
        Block block = state.getBlock();
        Collection<T> values = property.getValues();
        T value = state.get(property);
        int i = 0;
        do {
            value = cycle(values, value, inverse);
            i++;
        } while (i < values.size()
                && !Config.isPropertyValueAllowed(block, property.getName(), value.toString()));

        if (!Config.isPropertyValueAllowed(block, property.getName(), value.toString())) {
            return state;
        }
        return state.with(property, value);
    }
}
