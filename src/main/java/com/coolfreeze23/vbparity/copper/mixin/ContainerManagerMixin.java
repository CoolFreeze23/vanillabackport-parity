package com.coolfreeze23.vbparity.copper.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.golem.copper_golem.container.ContainerManager;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.golem.copper_golem.container.ContainerUser;
import com.coolfreeze23.vbparity.copper.CopperChests;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AI-CPG-02: natively the golem's container at interaction tick 60 is a CompoundContainer for a double chest, whose
 * getEntitiesWithContainerOpen is the empty Container default, so stopOpen never runs; the halves close at the next
 * 5-tick opener recheck once the golem has cleared its opened-chest position. ContainerManager.stopOpen is only
 * called from that tick-60 step.
 */
@Mixin(ContainerManager.class)
public abstract class ContainerManagerMixin {
    @Inject(method = "stopOpen", at = @At("HEAD"), cancellable = true)
    private static void vbparity$doubleChestClosesOnRecheck(BlockEntity blockEntity, ContainerUser user, CallbackInfo ci) {
        if (blockEntity != null && CopperChests.isCombinedDouble(blockEntity, blockEntity.getLevel())) {
            ci.cancel();
        }
    }
}
