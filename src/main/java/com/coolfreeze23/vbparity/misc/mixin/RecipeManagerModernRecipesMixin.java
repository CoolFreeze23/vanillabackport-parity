package com.coolfreeze23.vbparity.misc.mixin;

import com.blackgear.platform.common.v2.recipe.DynamicRecipeManager;
import com.blackgear.vanillabackport.common.level.recipes.RecipeStrategy;
import com.blackgear.vanillabackport.core.VanillaBackport;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Platform only lets VB's generated {@code minecraft:} recipes replace a file that comes from the vanilla pack, but
 * NeoForge ships its own {@code lodestone.json} and {@code lead.json} (tag versions of the 1.21.1 netherite and
 * slime-ball recipes), so VB's REPLACE mode never took effect for them. For the four VB recipe options that are not
 * IGNORE, put VB's modern recipe (native pattern and id) in place after Platform has generated it, when the file it
 * replaces is the vanilla one or NeoForge's copy (data packs still win). IGNORE keeps the 1.21.1 recipe, as VB intends.
 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerModernRecipesMixin {
    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;", ordinal = 0))
    private void vbp$modernRecipesWin(Map<ResourceLocation, JsonElement> recipes, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        vbp$put(recipes, resourceManager, "bundle", () -> VanillaBackport.COMMON_CONFIG.bundleRecipe.get());
        vbp$put(recipes, resourceManager, "lead", () -> VanillaBackport.COMMON_CONFIG.leadRecipe.get());
        vbp$put(recipes, resourceManager, "saddle", () -> VanillaBackport.COMMON_CONFIG.saddleRecipe.get());
        vbp$put(recipes, resourceManager, "lodestone", () -> VanillaBackport.COMMON_CONFIG.lodestoneRecipe.get());
    }

    @org.spongepowered.asm.mixin.Unique
    private static void vbp$put(Map<ResourceLocation, JsonElement> recipes, ResourceManager resourceManager, String name, Supplier<RecipeStrategy> strategy) {
        if (strategy.get() == RecipeStrategy.IGNORE) {
            return;
        }
        ResourceLocation id = ResourceLocation.withDefaultNamespace(name);
        JsonElement modern = DynamicRecipeManager.getRecipes().get(id);
        // Like Platform, never override a data pack or another mod; only the vanilla file or NeoForge's copy of it.
        String pack = resourceManager.getResource(ResourceLocation.withDefaultNamespace("recipe/" + name + ".json"))
                .map(resource -> resource.sourcePackId()).orElse("vanilla");
        if (modern != null && (pack.equals("vanilla") || pack.equals("mod/neoforge") || DynamicRecipeManager.isAllowedPack(pack))) {
            recipes.put(id, modern);
        }
    }
}
