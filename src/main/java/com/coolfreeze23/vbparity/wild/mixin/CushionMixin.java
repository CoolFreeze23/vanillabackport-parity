package com.coolfreeze23.vbparity.wild.mixin;

import com.blackgear.vanillabackport.client.registries.ModSoundEvents;
import com.blackgear.vanillabackport.common.level.entities.decoration.Cushion;
import com.coolfreeze23.vbparity.wild.TemporarilyInvulnerable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Cushion.class)
public abstract class CushionMixin extends BlockAttachedEntity {
    private CushionMixin(EntityType<? extends BlockAttachedEntity> type, Level level) {
        super(type, level);
    }

    @Shadow
    public abstract void destroyIfInFire(ServerLevel level);

    // ---- WILD-30 --------------------------------------------------------------------------------------------------

    /**
     * Native BlockAttachedEntity.tick runs Cushion.tickAtCheckInterval once every 101 ticks (the same cadence as
     * 1.21.1's survives() check), before the survives() check: lava (and other fluids) touching the cushion apply their
     * effects (lava: ignite + lavaHurt, which breaks it), then destroyIfInFire. VB checks fire every tick and never
     * reacts to lava. checkInterval == 100 here means this tick's super.tick() runs the interval check.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void vbparity$wild$tickAtCheckInterval(CallbackInfo ci) {
        if (this.level() instanceof ServerLevel serverLevel && ((BlockAttachedEntityAccessor) this).vbparity$wild$getCheckInterval() == 100) {
            BlockPos blockPos = this.blockPosition();
            FluidState fluid = serverLevel.getFluidState(blockPos);
            if (fluid.is(FluidTags.LAVA)) {
                AABB fluidBox = new AABB(
                    blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                    blockPos.getX() + 1, blockPos.getY() + fluid.getHeight(serverLevel, blockPos), blockPos.getZ() + 1
                );
                if (fluidBox.intersects(this.getBoundingBox())) {
                    this.lavaHurt();
                }
            }
            this.destroyIfInFire(serverLevel);
        }
    }

    @Redirect(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/decoration/Cushion;destroyIfInFire(Lnet/minecraft/server/level/ServerLevel;)V")
    )
    private void vbparity$wild$noFireCheckEveryTick(Cushion cushion, ServerLevel level) {
    }

    // ---- WILD-32 --------------------------------------------------------------------------------------------------

    /** Native: getY(0.6666666666666666) (VB: 0.67F). */
    @ModifyArg(
        method = "showBreakingParticles",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/decoration/Cushion;getY(D)D")
    )
    private double vbparity$wild$particleHeight(double fraction) {
        return 0.6666666666666666;
    }

    /** Native y spread getBbHeight() / 4 (VB: width / 4). sendParticles(type, x, y, z, count, xDist, yDist, zDist, speed). */
    @ModifyArg(
        method = "showBreakingParticles",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"),
        index = 6
    )
    private double vbparity$wild$particleYSpread(double ySpread) {
        return this.getBbHeight() / 4.0F;
    }

    /** Native thunderHit passes the lightning bolt to dropItem, which makes the dropped item invulnerable for 20 ticks. */
    @WrapOperation(
        method = "thunderHit",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/decoration/Cushion;dropItem(Lnet/minecraft/world/entity/Entity;)V")
    )
    private void vbparity$wild$dropCausedByLightning(Cushion cushion, Entity causedBy, Operation<Void> original, ServerLevel level, LightningBolt lightning) {
        original.call(cushion, lightning);
    }

    @WrapOperation(
        method = "dropItem",
        at = @At(value = "INVOKE", target = "Lcom/blackgear/vanillabackport/common/level/entities/decoration/Cushion;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;")
    )
    private ItemEntity vbparity$wild$lightningDropInvulnerable(Cushion cushion, ItemStack stack, Operation<ItemEntity> original, Entity causedBy) {
        ItemEntity item = original.call(cushion, stack);
        if (item != null && causedBy instanceof LightningBolt) {
            ((TemporarilyInvulnerable) item).vbparity$setInvulnerableTime(20);
        }
        return item;
    }

    /**
     * Native interact: secondary use or already ridden: PASS. Server and startRiding succeeds: sit sound, SUCCESS_SERVER
     * (1.21.1 SUCCESS on the server: the server swings the arm). Otherwise CONSUME (client side, or a failed mount).
     * VB returns SUCCESS on the client (client-side swing), CONSUME on the server, PASS on a failed mount.
     */
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void vbparity$wild$nativeInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.isSecondaryUseActive() || this.isVehicle()) {
            cir.setReturnValue(InteractionResult.PASS);
        } else if (!this.level().isClientSide() && player.startRiding(this)) {
            this.playSound(ModSoundEvents.CUSHION_SIT.get());
            cir.setReturnValue(InteractionResult.SUCCESS);
        } else {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}
