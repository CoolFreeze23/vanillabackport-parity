package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.api.modules.leash_behavior.LeashableCallback;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;

/** GHAST-37: an elastic lead pull stops a horse from grazing, as in the current game's AbstractHorse.onElasticLeashPull. */
@Mixin(AbstractHorse.class)
public abstract class AbstractHorseLeashMixin implements LeashableCallback {
    @Override
    public void vb$onElasticLeashPull() {
        AbstractHorse self = (AbstractHorse) (Object) this;
        self.checkSlowFallDistance();
        if (self.isEating()) {
            self.setEating(false);
        }
    }
}
