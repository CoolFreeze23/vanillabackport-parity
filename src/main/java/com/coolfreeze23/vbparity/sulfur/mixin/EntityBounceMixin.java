package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.common.api.extensions.entity.movement.TravelAwareEntity;
import com.blackgear.vanillabackport.common.registries.entities.ModAttributes;
import com.blackgear.vanillabackport.core.data.tags.ModBlockTags;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.Level;
import com.blackgear.vanillabackport.common.level.blocks.ShelfMushroomBlock;
import com.coolfreeze23.vbparity.sulfur.CollidedMovement;
import com.coolfreeze23.vbparity.sulfur.SulfurBounce;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * SULFUR-13. Native Entity#restituteMovementAfterCollisions for VB sulfur cubes and entities with BOUNCINESS > 0
 * (SulfurBounce.nativeRestitution). Runs right after VB's own handler at the same point and replaces its result:
 * x/z bounce with the entity restitution even on bounce-suppressing blocks, the floor bounce uses
 * max(restitution, block bounciness) (slime 1.0, beds and shelf mushrooms 0.75), and the drag is the plain air drag (no ground friction).
 */
@Mixin(Entity.class)
public abstract class EntityBounceMixin implements CollidedMovement {
    @Shadow
    public boolean verticalCollision;
    @Shadow
    public boolean verticalCollisionBelow;
    @Shadow
    public boolean horizontalCollision;

    @Unique
    private Vec3 vbp$velocityBeforeCollide;
    @Unique
    private Vec3 vbp$collidedMovement;
    @Unique
    private Vec3 vbp$anyBeforeCollide;
    @Unique
    private Vec3 vbp$anyCollided;
    @Unique
    private boolean vbp$xCollision;
    @Unique
    private boolean vbp$zCollision;
    @Unique
    private boolean vbp$movedVertically;

    @Shadow
    public abstract Vec3 getDeltaMovement();

    @Shadow
    public abstract void setDeltaMovement(double x, double y, double z);

    @Shadow
    public abstract Level level();

    @WrapOperation(
        method = "move",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 vbp$captureCollision(Entity self, Vec3 movement, Operation<Vec3> original) {
        if (!vbp$isBouncy(self)) {
            this.vbp$collidedMovement = null;
            this.vbp$anyBeforeCollide = this.getDeltaMovement();
            Vec3 collided = original.call(self, movement);
            this.vbp$anyCollided = collided;
            return collided;
        }
        this.vbp$anyBeforeCollide = this.getDeltaMovement();
        this.vbp$velocityBeforeCollide = this.getDeltaMovement();
        Vec3 collided = original.call(self, movement);
        this.vbp$collidedMovement = collided;
        this.vbp$anyCollided = collided;
        this.vbp$xCollision = !Mth.equal(movement.x, collided.x);
        this.vbp$zCollision = !Mth.equal(movement.z, collided.z);
        this.vbp$movedVertically = Math.abs(movement.y) > 0.0;
        return collided;
    }

    @WrapOperation(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tryCheckInsideBlocks()V"))
    private void vbp$nativeRestitution(Entity self, Operation<Void> original) {
        Vec3 movement = this.vbp$collidedMovement;
        this.vbp$collidedMovement = null;
        if (movement != null
            && self instanceof LivingEntity living
            && (!this.level().isClientSide() || self.isControlledByLocalInstance())
            && (this.vbp$movedVertically && this.verticalCollision || this.horizontalCollision)) {
            this.vbp$restitute(living, movement);
        }
        original.call(self);
    }

    @Unique
    private void vbp$restitute(LivingEntity living, Vec3 movement) {
        Vec3 current = this.vbp$velocityBeforeCollide;
        boolean suppressing = living.isSuppressingBounce();
        BlockState effectState = this.level().getBlockState(living.getOnPosLegacy());
        double restitution = suppressing ? 0.0 : vbp$attribute(living, ModAttributes.BOUNCINESS, 0.0);
        double x = this.vbp$xCollision ? -current.x * restitution : current.x;
        double z = this.vbp$zCollision ? -current.z * restitution : current.z;
        double y = current.y;
        boolean bounced = restitution > 0.0 && (this.vbp$xCollision || this.vbp$zCollision);
        if (this.verticalCollision) {
            double gravity = current.y <= 0.0 && living.hasEffect(MobEffects.SLOW_FALLING) ? Math.min(living.getGravity(), 0.01) : living.getGravity();
            if (this.verticalCollisionBelow) {
                // 26.2 cut-off is -dy < gravity; the shelf mushroom (26.3 content) uses 26.3's -dy <= gravity
                boolean resting = effectState.getBlock() instanceof ShelfMushroomBlock ? -current.y <= gravity : -current.y < gravity;
                restitution = !resting && !suppressing && !effectState.is(ModBlockTags.SUPPRESSES_BOUNCE)
                    ? Math.max(restitution, vbp$blockBounciness(effectState.getBlock()))
                    : 0.0;
            }

            double gravityCompensation;
            double effectiveDrag;
            if (restitution > 0.0) {
                double portionWithMovement = current.y != 0.0 ? movement.y / current.y : 0.0;
                gravityCompensation = portionWithMovement * gravity;
                effectiveDrag = Mth.lerp(portionWithMovement, 1.0, vbp$airDrag(living));
                bounced = true;
            } else {
                gravityCompensation = 0.0;
                effectiveDrag = 1.0;
            }
            y = (gravityCompensation - current.y) * effectiveDrag * restitution;
        }

        this.setDeltaMovement(x, y, z);
        if (bounced) {
            living.gameEvent(GameEvent.HIT_GROUND);
        }
    }

    @Unique
    private static boolean vbp$isBouncy(Entity entity) {
        return SulfurBounce.nativeRestitution(entity);
    }

    /**
     * TPHYS-AE01: the 1.21.1 slime and bed bounces (no compensation, bed 0.66) are skipped for entities that get the native
     * restitution below; Block's default (stop vertically) runs instead, which the restitution then replaces.
     */
    @WrapOperation(
        method = "move",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;updateEntityAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V")
    )
    private void vbp$noLegacyBounce(Block block, BlockGetter level, Entity entity, Operation<Void> original) {
        if ((block instanceof SlimeBlock || block instanceof BedBlock) && vbp$isBouncy(entity)) {
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
        } else {
            original.call(block, level, entity);
        }
    }

    /**
     * TPHYS-A02: natively stepOn runs from applyEffectsFromBlocks after travel, on the post-bounce velocity (so the slime
     * step slow-down is not overwritten by the restitution). For the same entities it moves to LivingEntityStepOnMixin.
     */
    @WrapOperation(
        method = "move",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;stepOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;)V")
    )
    private void vbp$deferStepOn(Block block, Level level, BlockPos pos, BlockState state, Entity entity, Operation<Void> original) {
        if (!(entity instanceof LivingEntity && vbp$isBouncy(entity))) {
            original.call(block, level, pos, state, entity);
        }
    }

    /** Native Block#getBounceRestitution: slime block 1.0, beds 0.75, everything else 0 (living entities, no 0.8 factor). */
    @Unique
    private static double vbp$blockBounciness(Block block) {
        if (block instanceof ShelfMushroomBlock) {
            return 0.75F; // TBLK-302 / TPHYS-B04: 26.3 Blocks.SHELF_MUSHROOM bounceRestitution(0.75F)
        }
        if (block == Blocks.SLIME_BLOCK) {
            return 1.0F;
        }
        return block instanceof BedBlock ? 0.75F : 0.0F;
    }

    /** Native LivingEntity#getAirDrag. */
    @Unique
    private static double vbp$airDrag(LivingEntity living) {
        float friction = living instanceof TravelAwareEntity mover && mover.omnidirectionalAirMover() ? 0.91F : 0.98F;
        float modifier = (float) vbp$attribute(living, ModAttributes.AIR_DRAG_MODIFIER, 1.0);
        return Mth.clamp(1.0F - (1.0F - friction) * modifier, 0.0F, 1.0F);
    }

    @Unique
    private static double vbp$attribute(LivingEntity living, Holder<Attribute> attribute, double fallback) {
        AttributeInstance instance = living.getAttribute(attribute);
        return instance != null ? instance.getValue() : fallback;
    }

    @Override
    public Vec3 vbparity$movementBeforeCollide() {
        return this.vbp$anyBeforeCollide;
    }

    @Override
    public Vec3 vbparity$collidedMovement() {
        return this.vbp$anyCollided;
    }
}
