package com.coolfreeze23.vbparity.sulfur.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mob.class)
public interface MobAccessor {
    @Accessor("persistenceRequired")
    boolean vbp$sulfurPersistenceRequired();

    @Accessor("armorDropChances")
    float[] vbp$armorDropChances();
}
