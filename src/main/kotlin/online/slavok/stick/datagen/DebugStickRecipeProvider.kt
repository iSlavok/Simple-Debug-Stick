package online.slavok.stick.datagen

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.minecraft.data.recipe.RecipeExporter
import net.minecraft.data.recipe.RecipeGenerator
import net.minecraft.data.recipe.SmithingTransformRecipeJsonBuilder
import net.minecraft.item.Items
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.Identifier
import online.slavok.stick.SimpleDebugStick
import java.util.concurrent.CompletableFuture

/**
 * Generates the single recipe this mod adds: a smithing-table upgrade of a plain stick
 * into a debug stick, gated behind netherite so it lands late-game rather than on day
 * one (upstream used a shapeless stick + chorus fruit craft).
 */
class DebugStickRecipeProvider(
    output: FabricDataOutput,
    registriesFuture: CompletableFuture<RegistryWrapper.WrapperLookup>,
) : FabricRecipeProvider(output, registriesFuture) {

    override fun getRecipeGenerator(
        registries: RegistryWrapper.WrapperLookup,
        exporter: RecipeExporter,
    ): RecipeGenerator = object : RecipeGenerator(registries, exporter) {
        override fun generate() {
            SmithingTransformRecipeJsonBuilder.create(
                Ingredient.ofItem(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.ofItem(Items.STICK),
                Ingredient.ofItem(Items.NETHERITE_INGOT),
                RecipeCategory.TOOLS,
                Items.DEBUG_STICK,
            )
                .criterion(hasItem(Items.NETHERITE_INGOT), conditionsFromItem(Items.NETHERITE_INGOT))
                .offerTo(
                    exporter,
                    RegistryKey.of(
                        RegistryKeys.RECIPE,
                        Identifier.of(SimpleDebugStick.MOD_ID, "debug_stick_smithing"),
                    ),
                )
        }
    }

    override fun getName(): String = "Simple Debug Stick Recipes"
}
