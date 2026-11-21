package com.coolfreeze23.vbparity.wild.mixin;

import com.blackgear.vanillabackport.client.registries.ModSoundEvents;
import com.blackgear.vanillabackport.common.level.blocks.ShelfMushroomBlock;
import com.blackgear.vanillabackport.common.api.extensions.entity.movement.TravelAwareEntity;
import com.blackgear.vanillabackport.common.registries.entities.ModAttributes;
import com.coolfreeze23.vbparity.sulfur.CollidedMovement;
import com.coolfreeze23.vbparity.sulfur.SulfurBounce;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShelfMushroomBlock.class)
public abstract class ShelfMushroomBlockMixin extends HorizontalDirectionalBlock {
    private ShelfMushroomBlockMixin(Properties properties) {
        super(properties);
    }

    /**
     * WILD-24 / TBLK-303 / TBLK-351. Native 26.3 Entity bounce on a shelf mushroom (restitution 0.75, x0.8 for non-living):
     * no bounce at all when -dy is at or below the entity's effective gravity (an entity resting on it stays grounded;
     * slow falling caps gravity at 0.01 for falling living entities); otherwise ShelfMushroomBlock.bounceOn plays
     * block.shelf_mushroom.bounce (1.0, 1.0) at getBlockPosBelowThatAffectsMyMovement() for every non-item, non-TNT entity,
     * and the entity emits GameEvent.BOUNCE. 1.21.1 has no BOUNCE; HIT_GROUND has the same vibration frequency (2) and
     * listener tags, so it stands in. Native gravity/drag compensation included (TPHYS-B01), from the collided movement EntityBounceMixin keeps.
     * Entities with VB's bounciness attribute are bounced by the sulfur area's EntityBounceMixin afterwards (which also
     * emits the vibration), so they get no second event here. The whole private method is replaced because the sound gate
     * is part of its if-condition.
     */
    @Inject(method = "bounceUp", at = @At("HEAD"), cancellable = true)
    private void vbparity$wild$nativeBounce(Entity entity, CallbackInfo ci) {
        ci.cancel();
        Vec3 movement = entity.getDeltaMovement();
        if (movement.y < 0.0) {
            double gravity = entity.getGravity();
            if (entity instanceof LivingEntity living && living.hasEffect(MobEffects.SLOW_FALLING)) {
                gravity = Math.min(gravity, 0.01);
            }
            if (-movement.y <= gravity) {
                entity.setDeltaMovement(movement.x, 0.0, movement.z);
                return;
            }
            // native restituteMovementAfterCollisions: y = (p * g - dy) * lerp(p, 1, airDrag) * restitution, p = collided dy / dy
            double restitution = 0.75 * (entity instanceof LivingEntity ? 1.0 : 0.8);
            Vec3 collided = ((CollidedMovement) entity).vbparity$collidedMovement();
            double portion = collided != null ? collided.y / movement.y : 0.0;
            double drag = Mth.lerp(portion, 1.0, vbparity$wild$airDrag(entity));
            entity.setDeltaMovement(movement.x, (portion * gravity - movement.y) * drag * restitution, movement.z);
            if (!entity.level().isClientSide) {
                if (!(entity instanceof ItemEntity) && !(entity instanceof PrimedTnt)) {
                    entity.level().playSound(
                        null, entity.getBlockPosBelowThatAffectsMyMovement(), ModSoundEvents.SHELF_MUSHROOM_BOUNCE.get(), SoundSource.BLOCKS, 1.0F, 1.0F
                    );
                }
                if (!vbparity$wild$vbBouncy(entity)) {
                    entity.gameEvent(GameEvent.HIT_GROUND);
                }
            }
        }
    }

    /** Native Entity#getAirDrag overrides that exist on 1.21.1 entity classes. */
    private static double vbparity$wild$airDrag(Entity entity) {
        if (entity instanceof LivingEntity living) {
            float friction = living instanceof TravelAwareEntity mover && mover.omnidirectionalAirMover() ? 0.91F : 0.98F;
            AttributeInstance modifier = living.getAttribute(ModAttributes.AIR_DRAG_MODIFIER);
            float m = modifier != null ? (float) modifier.getValue() : 1.0F;
            return Mth.clamp(1.0F - (1.0F - friction) * m, 0.0F, 1.0F);
        }
        if (entity instanceof AbstractArrow || entity instanceof LlamaSpit || entity instanceof ThrowableProjectile) {
            return 0.99F;
        }
        if (entity instanceof Boat) {
            return 1.0F;
        }
        if (entity instanceof AbstractMinecart) {
            return 0.95F;
        }
        return 0.98F;
    }

    private static boolean vbparity$wild$vbBouncy(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return SulfurBounce.nativeRestitution(living);
        }
        return false;
    }

    /** WILD-18. Native ShelfMushroomBlock.isPathfindable returns false. */
    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }
}
