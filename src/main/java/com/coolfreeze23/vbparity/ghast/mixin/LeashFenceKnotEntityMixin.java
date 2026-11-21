package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.api.modules.leash_behavior.LeashPhysics;
import com.coolfreeze23.vbparity.ghast.GhastSounds;
import com.coolfreeze23.vbparity.ghast.LeashRules;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GHAST-33: the current game's fence knot. Clicking it ties the mobs you hold to it; holding none, and not sneaking,
 * it hands the mobs tied to it back to you. A click that does neither does nothing (a click no longer destroys the
 * knot). Shears on a knot are already handled by VB's leash interaction, which drops every connection.
 * GHAST-32: knot sounds are the item.lead.* events.
 */
@Mixin(LeashFenceKnotEntity.class)
public abstract class LeashFenceKnotEntityMixin extends BlockAttachedEntity {
    protected LeashFenceKnotEntityMixin(EntityType<? extends BlockAttachedEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void vbp$interact(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!LeashRules.modernLeashes() || this.level().isClientSide()) {
            return;
        }

        LeashFenceKnotEntity self = (LeashFenceKnotEntity) (Object) this;
        boolean attachedMob = false;
        for (Leashable leashable : LeashPhysics.leashableLeashedTo(player)) {
            if (LeashPhysics.canAttachLeash(leashable, self)) {
                leashable.setLeashedTo(self, true);
                attachedMob = true;
            }
        }

        boolean anyDropped = false;
        if (!attachedMob && !player.isSecondaryUseActive()) {
            for (Leashable mob : LeashPhysics.leashableLeashedTo(self)) {
                if (LeashPhysics.canAttachLeash(mob, player)) {
                    mob.setLeashedTo(player, true);
                    anyDropped = true;
                }
            }
        }

        if (!attachedMob && !anyDropped) {
            cir.setReturnValue(InteractionResult.PASS);
        } else {
            this.gameEvent(GameEvent.BLOCK_ATTACH, player);
            this.playSound(GhastSounds.LEAD_TIED);
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @ModifyArg(method = "dropItem", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/decoration/LeashFenceKnotEntity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
    private SoundEvent vbp$untiedSound(SoundEvent sound) {
        return LeashRules.modernLeashes() ? GhastSounds.LEAD_UNTIED : sound;
    }

    @ModifyArg(method = "playPlacementSound", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/decoration/LeashFenceKnotEntity;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"))
    private SoundEvent vbp$tiedSound(SoundEvent sound) {
        return LeashRules.modernLeashes() ? GhastSounds.LEAD_TIED : sound;
    }
}
