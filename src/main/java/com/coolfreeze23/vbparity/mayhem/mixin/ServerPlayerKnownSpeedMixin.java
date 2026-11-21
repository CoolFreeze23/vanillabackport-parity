package com.coolfreeze23.vbparity.mayhem.mixin;

import com.coolfreeze23.vbparity.mayhem.KnownMovement;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MAYHEM-32: native ServerGamePacketListenerImpl.handleClientTickEnd sets the player's known movement to zero when a
 * client tick ended without a movement packet. The client sends a tick-end packet ({@link KnownMovement}); this keeps
 * the native state ({@code receivedMovementThisTick}) per player. VB's ServerPlayer.getKnownSpeed (spear charge,
 * sulfur cube pushes) and the other VB readers that go through {@link KnownMovement#of} read the reset value. The
 * vanilla field and getKnownMovement are left untouched.
 *
 * <p>A client that never sends the tick-end packet (no vbparity on it) falls back to reading zero after more than
 * {@link #VBP$STALE_TICKS} server ticks without a movement packet.
 */
@Mixin(value = ServerPlayer.class, priority = 1500)
public abstract class ServerPlayerKnownSpeedMixin extends Player implements KnownMovement.Tracker {
    /** Fallback only: ticks without a move packet before the known speed reads zero. */
    @Unique
    private static final int VBP$STALE_TICKS = 2;

    @Shadow
    @Final
    public MinecraftServer server;

    @Unique
    private int vbp$lastMovementTick = Integer.MIN_VALUE / 2;
    /** Native receivedMovementThisTick. */
    @Unique
    private boolean vbp$receivedMovementThisTick;
    /** True once a tick ended without movement, until the next movement packet (native setKnownMovement(ZERO)). */
    @Unique
    private boolean vbp$movementReset;
    @Unique
    private boolean vbp$sendsTickEnd;

    protected ServerPlayerKnownSpeedMixin(Level level, BlockPos pos, float yRot, GameProfile profile) {
        super(level, pos, yRot, profile);
    }

    @Inject(method = "setKnownMovement", at = @At("HEAD"))
    private void vbp$stampMovement(Vec3 movement, CallbackInfo ci) {
        this.vbp$lastMovementTick = this.server.getTickCount();
        this.vbp$receivedMovementThisTick = true;
        this.vbp$movementReset = false;
    }

    @Override
    public void vbp$clientTickEnd() {
        this.vbp$sendsTickEnd = true;
        if (!this.vbp$receivedMovementThisTick) {
            this.vbp$movementReset = true;
        }
        this.vbp$receivedMovementThisTick = false;
    }

    @Override
    public boolean vbp$knownMovementIsReset() {
        if (this.vbp$sendsTickEnd) {
            return this.vbp$movementReset;
        }
        return this.server.getTickCount() - this.vbp$lastMovementTick > VBP$STALE_TICKS;
    }

    @ModifyReturnValue(method = "getKnownSpeed", at = @At("RETURN"), remap = false)
    private Vec3 vbp$resetSpeedIsZero(Vec3 original) {
        Entity vehicle = this.getVehicle();
        if (vehicle != null && vehicle.getControllingPassenger() != this) {
            return original;
        }
        return this.vbp$knownMovementIsReset() ? Vec3.ZERO : original;
    }
}
