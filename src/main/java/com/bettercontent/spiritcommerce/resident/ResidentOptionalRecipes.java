package com.bettercontent.spiritcommerce.resident;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

/** Typed adapters, loaded only when a recipe from the corresponding optional mod exists. */
final class ResidentOptionalRecipes {
    private ResidentOptionalRecipes() {}

    static int woodcuttingCount(Recipe<?> recipe) { return Hexerei.count(recipe); }
    static boolean canCut(Recipe<?> recipe, List<ItemStack> tools) { return FarmersDelight.canCut(recipe, tools); }
    static void wearCuttingTool(Recipe<?> recipe, List<ItemStack> tools) { FarmersDelight.wearTool(recipe, tools); }
    static ItemStack cookingContainer(Recipe<?> recipe) { return FarmersDelight.container(recipe); }

    private static final class Hexerei {
        static int count(Recipe<?> recipe) {
            return Math.max(1, Math.min(64, ((net.joefoxe.hexerei.data.recipes.WoodcutterRecipe) recipe).ingredientCount));
        }
    }

    private static final class FarmersDelight {
        static boolean canCut(Recipe<?> recipe, List<ItemStack> tools) {
            var cutting = (vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe) recipe;
            // Chance byproducts cannot be quoted as deterministic barter goods.
            if (cutting.getRollableResults().size() != 1
                    || cutting.getRollableResults().get(0).getChance() < 1.0F) return false;
            return tools.stream().anyMatch(cutting.getTool()::test);
        }
        static ItemStack container(Recipe<?> recipe) {
            return ((vectorwing.farmersdelight.common.crafting.CookingPotRecipe) recipe).getOutputContainer();
        }
        static void wearTool(Recipe<?> recipe, List<ItemStack> tools) {
            var cutting = (vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe) recipe;
            for (int i = 0; i < tools.size(); i++) {
                ItemStack stack = tools.get(i);
                if (!cutting.getTool().test(stack)) continue;
                if (stack.isDamageableItem()) {
                    stack.setDamageValue(stack.getDamageValue() + 1);
                    if (stack.getDamageValue() >= stack.getMaxDamage()) tools.set(i, ItemStack.EMPTY);
                }
                return;
            }
        }
    }
}
