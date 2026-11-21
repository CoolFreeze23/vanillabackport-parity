package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.common.level.block_entities.PotentSulfurBlockEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PotentSulfurBlockEntity.class)
public abstract class PotentSulfurBlockEntityMixin {
    // SULFUR-25, server half. lambda$static$5 is SERVER_LAUNCH_ENTITY_TICKER (the only lambda calling getRootVehicle).

    /** Native launches each entity in the column itself; passengers are then skipped by the !isPassenger() check. */
    @Redirect(method = "lambda$static$5", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getRootVehicle()Lnet/minecraft/world/entity/Entity;"))
    private static Entity vbp$launchOnlyTheEntityInside(Entity target) {
        return target;
    }

    /**
     * Native canSimulateMovement() on the server is false for players (Player.isClientAuthoritative() is true) and for
     * vehicles a player controls: their client moves them and runs the launch itself (GeyserLaunch). Everything else is
     * launched server-side and marked needsSync (1.21.1 hasImpulse) only when it was pushed (TPHYS-B03).
     */
    @WrapOperation(method = "lambda$static$5", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;addDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private static void vbp$skipPlayerDrivenVehicles(Entity entity, Vec3 impulse, Operation<Void> original) {
        if (!(entity instanceof Player) && !(entity.getControllingPassenger() instanceof Player)) {
            original.call(entity, impulse);
            entity.hasImpulse = true;
        }
    }

    /** Native sets needsSync inside the push instead of forcing a motion packet every tick (hurtMarked). */
    @Redirect(method = "lambda$static$5", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;hurtMarked:Z", opcode = Opcodes.PUTFIELD))
    private static void vbp$normalImpulse(Entity entity, boolean value) {
    }

    /** SULFUR-26. Native noxious gas line of sight uses collision shapes (glass, ice, slime and honey block it). */
    @ModifyArg(
        method = "haveLineOfSight",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/ClipContext;<init>(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/ClipContext$Block;Lnet/minecraft/world/level/ClipContext$Fluid;Lnet/minecraft/world/phys/shapes/CollisionContext;)V"
        ),
        index = 2
    )
    private static ClipContext.Block vbp$colliderLineOfSight(ClipContext.Block block) {
        return ClipContext.Block.COLLIDER;
    }
}
