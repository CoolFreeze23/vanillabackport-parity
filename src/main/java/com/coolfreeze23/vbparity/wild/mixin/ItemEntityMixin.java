package com.coolfreeze23.vbparity.wild.mixin;

import com.coolfreeze23.vbparity.wild.TemporarilyInvulnerable;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Temporary invulnerability for item entities, as native Entity.invulnerableTime (see TemporarilyInvulnerable). */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin implements TemporarilyInvulnerable {
    @Unique
    private int vbparity$invulnerableTime;

    @Override
    public void vbparity$setInvulnerableTime(int ticks) {
        this.vbparity$invulnerableTime = ticks;
    }

    @Override
    public int vbparity$getInvulnerableTime() {
        return this.vbparity$invulnerableTime;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void vbparity$wild$countDownInvulnerability(CallbackInfo ci) {
        if (this.vbparity$invulnerableTime > 0) {
            this.vbparity$invulnerableTime--;
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void vbparity$wild$temporarilyInvulnerable(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.vbparity$invulnerableTime > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.isCreativePlayer()) {
            cir.setReturnValue(false);
        }
    }
}
