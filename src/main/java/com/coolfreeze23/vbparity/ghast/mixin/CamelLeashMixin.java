package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.api.modules.leash_behavior.LeashableCallback;
import net.minecraft.world.entity.animal.camel.Camel;
import org.spongepowered.asm.mixin.Mixin;

/** GHAST-37: an elastic lead pull stops grazing and stands a sitting camel up (the current game's Camel.onElasticLeashPull). */
@Mixin(Camel.class)
public abstract class CamelLeashMixin implements LeashableCallback {
    @Override
    public void vb$onElasticLeashPull() {
        Camel self = (Camel) (Object) this;
        self.checkSlowFallDistance();
        if (self.isEating()) {
            self.setEating(false);
        }

        if (self.isCamelSitting() && !self.isInPoseTransition() && self.canCamelChangePose()) {
            self.standUp();
        }
    }
}
