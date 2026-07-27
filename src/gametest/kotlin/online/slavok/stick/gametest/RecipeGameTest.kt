package online.slavok.stick.gametest

import net.fabricmc.fabric.api.gametest.v1.GameTest
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.recipe.SmithingRecipe
import net.minecraft.recipe.input.SmithingRecipeInput
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.test.TestContext
import net.minecraft.util.Identifier
import online.slavok.stick.SimpleDebugStick

/**
 * Boots a real server and checks the shipped datapack: the smithing recipe must load and
 * actually turn a stick into a debug stick. Reaching any of this at all also proves the
 * mixins applied — they are `required`, so a bad injection would abort startup.
 */
class RecipeGameTest {

    @GameTest
    fun smithingRecipeUpgradesStickToDebugStick(context: TestContext) {
        val server = context.world.server
        val key = RegistryKey.of(
            RegistryKeys.RECIPE,
            Identifier.of(SimpleDebugStick.MOD_ID, "debug_stick_smithing"),
        )

        val entry = server.recipeManager.get(key).orElseThrow {
            AssertionError("recipe $key was not loaded")
        }
        val recipe = entry.value as? SmithingRecipe
            ?: throw AssertionError("recipe $key is a ${entry.value.javaClass.simpleName}, not a smithing recipe")

        val input = SmithingRecipeInput(
            ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
            ItemStack(Items.STICK),
            ItemStack(Items.NETHERITE_INGOT),
        )
        if (!recipe.matches(input, context.world)) {
            throw AssertionError("template + stick + netherite ingot does not match the recipe")
        }

        val result = recipe.craft(input, server.registryManager)
        if (!result.isOf(Items.DEBUG_STICK)) {
            throw AssertionError("recipe produced ${result.item}, expected a debug stick")
        }

        // A plain stick with no template must not upgrade.
        val wrong = SmithingRecipeInput(
            ItemStack.EMPTY,
            ItemStack(Items.STICK),
            ItemStack(Items.NETHERITE_INGOT),
        )
        if (recipe.matches(wrong, context.world)) {
            throw AssertionError("recipe matched without the netherite upgrade template")
        }

        context.complete()
    }
}
