package com.coolfreeze23.vbparity.mayhem.mixin;

import com.coolfreeze23.vbparity.mayhem.MayhemParity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The persistence from MobInteractPersistenceMixin also applies when an interaction event (VB's modern lead handling,
 * for one) consumes the click before Entity.interact runs; natively those interactions happen inside interact.
 */
@Mixin(Player.class)
public abstract class PlayerInteractPersistenceMixin {
    @Inject(method = "interactOn", at = @At(value = "INVOKE",
        target = "Lnet/neoforged/neoforge/common/CommonHooks;onInteractEntity(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"))
    private void vbp$persistBeforeEvent(Entity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        MayhemParity.persistOnInteract(target);
    }
}
