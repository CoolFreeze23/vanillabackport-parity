package com.coolfreeze23.vbparity.ai.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.ZombieNautilus;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/**
 * AI-MNT-10: native Drowned.rideTick turns the rider's body with the PathfinderMob it rides. Applied to drowned on a
 * zombie nautilus only (the VB jockey); other drowned rides keep 1.21.1 behaviour.
 */
@Mixin(Drowned.class)
public abstract class DrownedRideMixin extends Zombie {
    protected DrownedRideMixin(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    public void rideTick() {
        super.rideTick();
        if (this.getControlledVehicle() instanceof ZombieNautilus vehicle) {
            this.yBodyRot = vehicle.yBodyRot;
        }
    }
}
