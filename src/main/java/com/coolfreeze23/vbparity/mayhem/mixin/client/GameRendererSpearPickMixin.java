package com.coolfreeze23.vbparity.mayhem.mixin.client;

import com.blackgear.vanillabackport.common.level.components.AttackRange;
import com.blackgear.vanillabackport.core.ModChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MAYHEM-38: native LocalPlayer.raycastHitResult reads the active item (the item in use, else the main hand; nothing for
 * spectators) and never picks spectators. VB's pick hook reads the main hand and picks with Entity::isPickable.
 * This runs before VB's hook at the same point (lower priority applies first) whenever a spear is involved, does the
 * whole pick with the native rules and cancels, so VB's hook does not run. Without an attack-range item in either
 * place it does nothing.
 */
@Mixin(value = GameRenderer.class, priority = 900)
public abstract class GameRendererSpearPickMixin {
    @Shadow
    @Final
    Minecraft minecraft;

    @Shadow
    private HitResult pick(Entity cameraEntity, double blockInteractionRange, double entityInteractionRange, float partialTick) {
        throw new AssertionError();
    }

    @Inject(method = "pick(F)V", at = @At("HEAD"), cancellable = true)
    private void vbp$nativeSpearPick(float partialTicks, CallbackInfo ci) {
        if (ModChecker.BETTER_COMBAT) {
            return;
        }
        Entity camera = this.minecraft.getCameraEntity();
        LocalPlayer player = this.minecraft.player;
        if (camera == null || this.minecraft.level == null || player == null) {
            return;
        }
        ItemStack active = player.isSpectator() ? ItemStack.EMPTY : player.isUsingItem() ? player.getUseItem() : player.getMainHandItem();
        AttackRange range = AttackRange.get(active);
        if (range == null && AttackRange.get(player.getMainHandItem()) == null) {
            return;
        }

        ci.cancel();
        this.minecraft.getProfiler().push("pick");
        double blockReach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        double entityReach = player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        HitResult hit;
        if (range != null) {
            hit = range.getClosestHit(camera, partialTicks, entity -> !entity.isSpectator() && entity.isPickable());
            if (hit instanceof BlockHitResult) {
                hit = vbp$filter(hit, camera.getEyePosition(partialTicks), blockReach);
            }
            if (hit == null || hit.getType() == HitResult.Type.MISS) {
                hit = this.pick(camera, blockReach, entityReach, partialTicks);
            }
            this.minecraft.hitResult = hit;
            this.minecraft.crosshairPickEntity = hit instanceof EntityHitResult entityHit
                && (entityHit.getEntity() instanceof LivingEntity || entityHit.getEntity() instanceof ItemFrame) ? entityHit.getEntity() : null;
        } else {
            // The spear is in the main hand but another item is in use: the vanilla pick, as natively.
            hit = this.pick(camera, blockReach, entityReach, partialTicks);
            this.minecraft.hitResult = hit;
            this.minecraft.crosshairPickEntity = hit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
        }
        this.minecraft.getProfiler().pop();
    }

    @Unique
    private static HitResult vbp$filter(HitResult hit, Vec3 from, double maxRange) {
        Vec3 location = hit.getLocation();
        if (!location.closerThan(from, maxRange)) {
            Direction direction = Direction.getNearest(location.x - from.x, location.y - from.y, location.z - from.z);
            return BlockHitResult.miss(location, direction, BlockPos.containing(location));
        }
        return hit;
    }
}
