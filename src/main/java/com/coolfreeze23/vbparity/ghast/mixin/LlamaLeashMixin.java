package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.api.modules.leash_behavior.LeashableCallback;
import net.minecraft.world.entity.animal.horse.Llama;
import org.spongepowered.asm.mixin.Mixin;

/** GHAST-35: llamas (and trader llamas) use one lead, not four, as in the current game's Llama.supportQuadLeash. */
@Mixin(Llama.class)
public abstract class LlamaLeashMixin implements LeashableCallback {
    @Override
    public boolean vb$supportsQuadLeash() {
        return false;
    }
}
