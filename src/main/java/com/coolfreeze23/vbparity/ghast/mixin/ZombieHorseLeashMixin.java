package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.api.modules.leash_behavior.LeashPhysics;
import com.blackgear.vanillabackport.common.api.modules.leash_behavior.LeashableCallback;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

/** GHAST-36: the zombie horse's four lead attachment points, from the current game's ZombieHorse.getQuadLeashOffsets. */
@Mixin(ZombieHorse.class)
public abstract class ZombieHorseLeashMixin implements LeashableCallback {
    @Override
    public Vec3[] vb$getQuadLeashOffsets() {
        return LeashPhysics.createQuadOffsets((Entity) (Object) this, 0.04, 0.41, 0.18, 0.73);
    }
}
