package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.core.data.tags.ModItemTags;
import com.coolfreeze23.vbparity.sulfur.SulfurCubeDispensing;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * SULFUR-09. Native DispenserBlock#getDefaultDispenseMethod: an item with no registered behaviour that is sulfur-cube
 * swallowable is fed into a cube in front; the TNT behaviour also tries to feed a cube before priming.
 * DropperBlock overrides getDispenseMethod, so droppers are unaffected, as in native.
 */
@Mixin(DispenserBlock.class)
public abstract class DispenserBlockMixin {
    @Shadow
    @Final
    private static net.minecraft.core.dispenser.DefaultDispenseItemBehavior DEFAULT_BEHAVIOR;

    @ModifyReturnValue(method = "getDispenseMethod", at = @At("RETURN"))
    private DispenseItemBehavior vbp$sulfurCubeFeeding(DispenseItemBehavior behavior, Level level, ItemStack stack) {
        if (behavior == DEFAULT_BEHAVIOR && stack.isItemEnabled(level.enabledFeatures()) && stack.is(ModItemTags.SULFUR_CUBE_SWALLOWABLE)) {
            return SulfurCubeDispensing.FEED;
        }
        if (stack.is(Items.TNT) && behavior != DEFAULT_BEHAVIOR && behavior != DispenseItemBehavior.NOOP) {
            return SulfurCubeDispensing.wrapTnt(behavior);
        }
        return behavior;
    }
}
