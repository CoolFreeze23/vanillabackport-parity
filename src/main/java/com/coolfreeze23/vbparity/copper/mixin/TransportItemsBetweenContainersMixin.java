package com.coolfreeze23.vbparity.copper.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.golem.copper_golem.TransportItemsBetweenContainers;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** COPPER-04: native golems only consider chest block entities as transport targets. */
@Mixin(TransportItemsBetweenContainers.class)
public abstract class TransportItemsBetweenContainersMixin {
    @WrapOperation(method = "getTransportTarget", at = @At(value = "CONSTANT", args = "classValue=net/minecraft/world/Container"))
    private boolean vbparity$onlyChests(Object blockEntity, Operation<Boolean> original) {
        return original.call(blockEntity) && blockEntity instanceof ChestBlockEntity;
    }
}
