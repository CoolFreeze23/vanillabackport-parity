package com.coolfreeze23.vbparity.misc.mixin;

import com.blackgear.vanillabackport.common.integrations.dispenser.BoatDispenseBehavior;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Native BoatDispenseItemBehavior: {@code EntityType.createDefaultStackConfig(level, stack, null).apply(boat)}. */
@Mixin(BoatDispenseBehavior.class)
public abstract class BoatDispenseBehaviorMixin {
    @ModifyVariable(method = "execute", at = @At("STORE"))
    private Boat vbp$applyStackConfig(Boat boat, @Local(argsOnly = true) BlockSource source, @Local(argsOnly = true) ItemStack stack) {
        EntityType.<Boat>createDefaultStackConfig(source.level(), stack, null).accept(boat);
        return boat;
    }
}
