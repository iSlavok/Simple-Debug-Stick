package online.slavok.stick.gametest;

import online.slavok.stick.SimpleDebugStick;
//? if >=1.21.2 {
import net.fabricmc.fabric.api.gametest.v1.GameTest;
//?} else {
/*import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;*/
//?}
//? if >=1.22 {
/*import net.minecraft.gametest.framework.GameTestHelper;*/
//?} else {
import net.minecraft.test.TestContext;
//?}
//? if >=1.21.2 && <1.22 {
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
//?}
//? if <1.22 {
import net.minecraft.util.Identifier;
//?}

/**
 * Boots a real headless server. Reaching the test at all proves the required
 * DebugStickItem mixin applied — a mis-targeted required mixin crashes on class-load,
 * which matters most on the non-remapping 26+ toolchain where the mixin target is not
 * validated at compile time.
 *
 * <p>On yarn-mapped versions it additionally asserts the shipped smithing recipe parsed
 * and loaded (a recipe authored in the wrong JSON format for the version would be
 * dropped during data loading). The registry API is Mojang-mapped on 26+, so there the
 * test is apply-only.
 *
 * <p>Only the GameTest harness (interface / annotation / context type / finish call) is
 * branched per version; the body touches only stable registry API.
 */
//? if >=1.21.2 {
public class RecipeGameTest {
//?} else {
/*public class RecipeGameTest implements FabricGameTest {*/
//?}

    //? if >=1.21.2 {
    @GameTest
    //?} elif >=1.19 {
    /*@GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE)*/
    //?} else {
    /*@GameTest(structureName = FabricGameTest.EMPTY_STRUCTURE)*/
    //?}
    //? if >=1.22 {
    /*public void recipeLoaded(GameTestHelper context) {
        // Booting with the required mixin applied is the assertion on 26+.
        context.succeed();
    }*/
    //?} else {
    public void recipeLoaded(TestContext context) {
        // Identifier.of(...) is the factory since 1.19; before that it is a constructor.
        //? if >=1.19 {
        Identifier id = Identifier.of(SimpleDebugStick.MOD_ID, "debug_stick_smithing");
        //?} else {
        /*Identifier id = new Identifier(SimpleDebugStick.MOD_ID, "debug_stick_smithing");*/
        //?}
        // RecipeManager keys on RegistryKey since 1.21.2; on the bare Identifier before.
        //? if >=1.21.2 {
        boolean loaded = context.getWorld().getServer().getRecipeManager()
                .get(RegistryKey.of(RegistryKeys.RECIPE, id)).isPresent();
        //?} else {
        /*boolean loaded = context.getWorld().getServer().getRecipeManager()
                .get(id).isPresent();*/
        //?}
        if (!loaded) throw new RuntimeException("recipe " + id + " was not loaded");
        context.complete();
    }
    //?}
}
