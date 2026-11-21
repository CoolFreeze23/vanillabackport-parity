package com.coolfreeze23.vbparity.misc.mixin;

import com.blackgear.vanillabackport.common.level.recipes.RecipeStrategy;
import com.blackgear.vanillabackport.common.level.recipes.VanillaDynamicRecipes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * VB's default "APPEND" keeps the 1.21.1 netherite lodestone and slime-ball lead recipes and saves the modern
 * ones under {@code vanillabackport:} ids. Native has only the modern recipe, under the {@code minecraft:} id,
 * which is exactly what VB writes in "REPLACE" mode. "IGNORE" (keep vanilla 1.21.1) and "REPLACE" are left as they are.
 */
@Mixin(VanillaDynamicRecipes.class)
public abstract class VanillaDynamicRecipesMixin {
    @ModifyVariable(method = "configurableRecipe", at = @At("HEAD"), argsOnly = true)
    private static RecipeStrategy vbp$appendMeansNative(RecipeStrategy strategy) {
        return strategy == RecipeStrategy.APPEND ? RecipeStrategy.REPLACE : strategy;
    }
}
