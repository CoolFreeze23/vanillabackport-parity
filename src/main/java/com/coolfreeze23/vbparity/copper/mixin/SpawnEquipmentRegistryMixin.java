package com.coolfreeze23.vbparity.copper.mixin;

import com.blackgear.vanillabackport.common.api.SpawnEquipmentRegistry;
import com.blackgear.vanillabackport.core.VanillaBackport;
import com.coolfreeze23.vbparity.copper.CopperEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** COPPER-19: copper is its own native tier in the armor roll, so VB's per-UUID gold-to-copper swap is off there. */
@Mixin(SpawnEquipmentRegistry.class)
public abstract class SpawnEquipmentRegistryMixin {
    @Inject(method = "tryGetCustomEquipment", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vbparity$nativeCopperTier(Mob mob, EquipmentSlot slot, int tier, CallbackInfoReturnable<Item> cir) {
        if (CopperEquipment.inRoll() && VanillaBackport.COMMON_CONFIG.hasCopperToolSet.get()) {
            cir.setReturnValue(null);
        }
    }
}
