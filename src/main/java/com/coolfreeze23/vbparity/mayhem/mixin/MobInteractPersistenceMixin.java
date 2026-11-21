package com.coolfreeze23.vbparity.mayhem.mixin;

import com.coolfreeze23.vbparity.mayhem.MayhemParity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Natively camel husks, zombie horses and (zombie) nautiluses become persistent on any player interaction: they
 * override Entity.interact with setPersistenceRequired() before anything else. VB only did it in mobInteract, which
 * leashing, name tags and spawn eggs never reach.
 */
@Mixin(Mob.class)
public abstract class MobInteractPersistenceMixin {
    @Inject(method = "interact", at = @At("HEAD"))
    private void vbp$persistOnInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        MayhemParity.persistOnInteract((Mob) (Object) this);
    }

    /** SPAWN-09: native Mob.requiresCustomPersistence is isPassenger() || isLeashed(); scoped to the VB mounts. */
    @Inject(method = "requiresCustomPersistence", at = @At("RETURN"), cancellable = true)
    private void vbp$leashedMountsStay(CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;
        if (!cir.getReturnValueZ() && self.isLeashed() && MayhemParity.isVbMount(self)) {
            cir.setReturnValue(true);
        }
    }
}
