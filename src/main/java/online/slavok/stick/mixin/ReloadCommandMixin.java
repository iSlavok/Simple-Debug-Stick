package online.slavok.stick.mixin;

//? if >=1.22 {
/*import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.ReloadCommand;
import online.slavok.stick.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

// Minecraft 26+ (Mojang names): ReloadCommand.reloadPacks(Collection, CommandSourceStack).
@Mixin(ReloadCommand.class)
public class ReloadCommandMixin {

    @Inject(method = "reloadPacks", at = @At("RETURN"))
    private static void simpleDebugStick$reloadConfig(
            Collection<String> dataPacks,
            CommandSourceStack source,
            CallbackInfo ci
    ) {
        Config.loadOrCreate();
    }
}
*///?} else {
import net.minecraft.server.command.ReloadCommand;
import net.minecraft.server.command.ServerCommandSource;
import online.slavok.stick.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

/** Picks up config edits on {@code /reload}, so operators need not restart the server. */
@Mixin(ReloadCommand.class)
public class ReloadCommandMixin {

    @Inject(method = "tryReloadDataPacks", at = @At("RETURN"))
    private static void simpleDebugStick$reloadConfig(
            Collection<String> dataPacks,
            ServerCommandSource source,
            CallbackInfo ci
    ) {
        Config.loadOrCreate();
    }
}
//?}
