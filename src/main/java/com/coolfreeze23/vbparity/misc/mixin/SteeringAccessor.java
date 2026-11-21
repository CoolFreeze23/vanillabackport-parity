package com.coolfreeze23.vbparity.misc.mixin;

import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Strider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Pig.class, Strider.class})
public interface SteeringAccessor {
    @Accessor("steering")
    ItemBasedSteering vbp$steering();
}
