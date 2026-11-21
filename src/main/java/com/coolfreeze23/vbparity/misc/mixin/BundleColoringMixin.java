package com.coolfreeze23.vbparity.misc.mixin;

import com.blackgear.vanillabackport.common.api.modules.bundle_ui.BundleColoring;
import com.blackgear.vanillabackport.common.api.modules.bundle_ui.BundleFeatures;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Native crafting_transmute refuses a bundle + dye that would produce the same bundle (TransmuteRecipe.matches). */
@Mixin(BundleColoring.class)
public abstract class BundleColoringMixin {
    @ModifyReturnValue(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z", at = @At("RETURN"))
    private boolean vbp$rejectSameColour(boolean matches, CraftingInput input, Level level) {
        if (!matches) {
            return false;
        }
        ItemStack bundle = ItemStack.EMPTY;
        DyeItem dye = null;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.getItem() instanceof DyeItem dyeItem) {
                dye = dyeItem;
            } else if (!stack.isEmpty()) {
                bundle = stack;
            }
        }
        return dye == null || !bundle.is(BundleFeatures.getByColor(dye.getDyeColor()));
    }
}
