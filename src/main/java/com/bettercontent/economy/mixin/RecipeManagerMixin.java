package com.bettercontent.economy.mixin;

import com.bettercontent.economy.recipe.CoinRecipeFilter;
import com.google.gson.JsonElement;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    @ModifyVariable(method = "apply", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Map<ResourceLocation, JsonElement> betterContentEconomy$removeCoinRecipes(
            Map<ResourceLocation, JsonElement> recipes) {
        return CoinRecipeFilter.filter(recipes);
    }
}
