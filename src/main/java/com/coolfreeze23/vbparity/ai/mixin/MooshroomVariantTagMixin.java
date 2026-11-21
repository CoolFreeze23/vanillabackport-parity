package com.coolfreeze23.vbparity.ai.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.animal.MushroomCow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** AI-VAR-02: native mooshrooms have no cow variant, so no "variant" key is saved for them. */
@Mixin(MushroomCow.class)
public abstract class MooshroomVariantTagMixin {
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void vbp$noCowVariant(CompoundTag tag, CallbackInfo ci) {
        tag.remove("variant");
    }
}
