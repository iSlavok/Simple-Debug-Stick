package online.slavok.stick.mixin;

//? if >=1.22 {
/*import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DebugStickItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import online.slavok.stick.config.Config;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

// Minecraft 26+ (unobfuscated / Mojang names). The debug-stick interaction method is
// named handleInteraction here and takes a ServerPlayer; the selected property lives in
// a DebugStickState component keyed by the block's Holder.
@Mixin(DebugStickItem.class)
public abstract class DebugStickMixin extends Item {
    public DebugStickMixin(Item.Properties settings) {
        super(settings);
    }

    @Shadow
    private static void message(ServerPlayer player, Component message) {}

    @Shadow
    private static <T extends Comparable<T>> String getNameHelper(BlockState state, Property<T> property) {
        return null;
    }

    @Shadow
    private static <T> T getRelative(Iterable<T> values, @Nullable T value, boolean inverse) {
        return null;
    }

    @Inject(at = @At("HEAD"), method = "handleInteraction", cancellable = true)
    private void simpleDebugStick$filterUse(
            ServerPlayer player,
            BlockState state,
            LevelAccessor world,
            BlockPos pos,
            boolean update,
            ItemStack stack,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (player.canUseGameMasterBlocks()) {
            return;
        }

        Block block = state.getBlock();
        StateDefinition<Block, BlockState> stateManager = block.getStateDefinition();
        Collection<Property<?>> properties = stateManager.getProperties();
        String blockId = Config.blockId(block);

        if (!Config.isBlockAllowed(block) || properties.isEmpty()) {
            simpleDebugStick$deny(player, blockId, cir);
            return;
        }

        Holder<Block> holder = block.builtInRegistryHolder();
        DebugStickState stateComponent = stack.get(DataComponents.DEBUG_STICK_STATE);
        Property<?> property = stateComponent == null ? null : stateComponent.properties().get(holder);

        if (update) {
            if (property == null) {
                property = simpleDebugStick$nextAllowedProperty(properties, null, block, false);
            }
            if (!Config.isPropertyAllowed(property.getName(), block)) {
                simpleDebugStick$deny(player, blockId, cir);
                return;
            }

            BlockState newState =
                    simpleDebugStick$nextAllowedState(state, property, player.isSecondaryUseActive());
            world.setBlock(pos, newState, 18);
            message(player, simpleDebugStick$text(
                    this.getDescriptionId() + ".update",
                    property.getName(),
                    getNameHelper(newState, property)
            ));
        } else {
            property = simpleDebugStick$nextAllowedProperty(
                    properties, property, block, player.isSecondaryUseActive());
            if (!Config.isPropertyAllowed(property.getName(), block)) {
                simpleDebugStick$deny(player, blockId, cir);
                return;
            }
            if (stateComponent != null) {
                stack.set(DataComponents.DEBUG_STICK_STATE, stateComponent.withProperty(holder, property));
            }
            message(player, simpleDebugStick$text(
                    this.getDescriptionId() + ".select",
                    property.getName(),
                    getNameHelper(state, property)
            ));
        }
        cir.setReturnValue(true);
    }

    @Unique
    private void simpleDebugStick$deny(ServerPlayer player, String blockId, CallbackInfoReturnable<Boolean> cir) {
        message(player, simpleDebugStick$text(this.getDescriptionId() + ".empty", blockId));
        cir.setReturnValue(false);
    }

    @Unique
    private Component simpleDebugStick$text(String key, Object... args) {
        return Component.translatable(key, args);
    }

    @Unique
    private Property<?> simpleDebugStick$nextAllowedProperty(
            Collection<Property<?>> properties,
            @Nullable Property<?> property,
            @Nullable Block block,
            boolean inverse
    ) {
        int i = 0;
        do {
            property = getRelative(properties, property, inverse);
            i++;
        } while (i < properties.size() && !Config.isPropertyAllowed(property.getName(), block));
        return property;
    }

    @Unique
    private <T extends Comparable<T>> BlockState simpleDebugStick$nextAllowedState(
            BlockState state,
            Property<T> property,
            boolean inverse
    ) {
        Block block = state.getBlock();
        Collection<T> values = property.getPossibleValues();
        T value = state.getValue(property);
        int i = 0;
        do {
            value = getRelative(values, value, inverse);
            i++;
        } while (i < values.size()
                && !Config.isPropertyValueAllowed(block, property.getName(), value.toString()));

        if (!Config.isPropertyValueAllowed(block, property.getName(), value.toString())) {
            return state;
        }
        return state.setValue(property, value);
    }
}
*///?} else {
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
//? if >=1.20.5 {
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DebugStickStateComponent;
import net.minecraft.registry.entry.RegistryEntry;
//?} else {
/*import net.minecraft.nbt.NbtCompound;
*///?}
//? if <1.19 {
/*import net.minecraft.text.TranslatableText;
*///?}
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DebugStickItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
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
 *
 * <p>The private {@code use} helper has the same signature across all yarn-mapped
 * versions; only where the selected property is stored differs — a
 * {@code DebugStickStateComponent} since 1.20.5, an {@code NbtCompound} sub-tag before it.
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
        StateManager<Block, BlockState> stateManager = block.getStateManager();
        Collection<Property<?>> properties = stateManager.getProperties();
        String blockId = Config.blockId(block);

        if (!Config.isBlockAllowed(block) || properties.isEmpty()) {
            simpleDebugStick$deny(player, blockId, cir);
            return;
        }

        // The selected property is remembered per block on the stack
        // (https://minecraft.wiki/w/Debug_Stick): a component since 1.20.5, an NBT
        // sub-tag before it.
        //? if >=1.20.5 {
        RegistryEntry<Block> registryEntry = state.getRegistryEntry();
        DebugStickStateComponent stateComponent = stack.get(DataComponentTypes.DEBUG_STICK_STATE);
        Property<?> property = stateComponent == null ? null : stateComponent.properties().get(registryEntry);
        //?} else {
        /*NbtCompound debugNbt = stack.getOrCreateSubNbt("DebugProperty");
        Property<?> property = stateManager.getProperty(debugNbt.getString(blockId));
        *///?}

        if (update) {
            if (property == null) {
                property = simpleDebugStick$nextAllowedProperty(properties, null, block, false);
            }
            if (!Config.isPropertyAllowed(property.getName(), block)) {
                simpleDebugStick$deny(player, blockId, cir);
                return;
            }

            BlockState newState =
                    simpleDebugStick$nextAllowedState(state, property, player.shouldCancelInteraction());
            world.setBlockState(pos, newState, 18);
            sendMessage(player, simpleDebugStick$text(
                    this.getTranslationKey() + ".update",
                    property.getName(),
                    getValueString(newState, property)
            ));
        } else {
            property = simpleDebugStick$nextAllowedProperty(
                    properties, property, block, player.shouldCancelInteraction());
            if (!Config.isPropertyAllowed(property.getName(), block)) {
                simpleDebugStick$deny(player, blockId, cir);
                return;
            }
            //? if >=1.20.5 {
            if (stateComponent != null) {
                stack.set(DataComponentTypes.DEBUG_STICK_STATE, stateComponent.with(registryEntry, property));
            }
            //?} else {
            /*debugNbt.putString(blockId, property.getName());
            *///?}
            sendMessage(player, simpleDebugStick$text(
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
            String blockId,
            CallbackInfoReturnable<Boolean> cir
    ) {
        sendMessage(player, simpleDebugStick$text(this.getTranslationKey() + ".empty", blockId));
        cir.setReturnValue(false);
    }

    /** Builds a translatable message. {@code Text.translatable} only exists since 1.19. */
    @Unique
    private Text simpleDebugStick$text(String key, Object... args) {
        //? if >=1.19 {
        return Text.translatable(key, args);
        //?} else {
        /*return new TranslatableText(key, args);*/
        //?}
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
//?}
