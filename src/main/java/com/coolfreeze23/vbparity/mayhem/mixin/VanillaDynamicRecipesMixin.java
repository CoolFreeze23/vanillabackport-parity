package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.common.level.recipes.VanillaDynamicRecipes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** MAYHEM-42: native gold/iron nugget blasting recipes for spears and nautilus armour use the blasting default, 100 ticks. */
@Mixin(value = VanillaDynamicRecipes.class, remap = false)
public abstract class VanillaDynamicRecipesMixin {
    @ModifyArg(
        method = "buildMountsOfMayhemRecipes",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/data/recipes/SimpleCookingRecipeBuilder;blasting(Lnet/minecraft/world/item/crafting/Ingredient;Lnet/minecraft/data/recipes/RecipeCategory;Lnet/minecraft/world/level/ItemLike;FI)Lnet/minecraft/data/recipes/SimpleCookingRecipeBuilder;"),
        index = 4
    )
    private static int vbp$blastingTime(int cookingTime) {
        return 100;
    }
}
