package com.coolfreeze23.vbparity.variants.mixin;

import com.coolfreeze23.vbparity.variants.BabyDimensions;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

/** VARIANTS-13: native puppy dimensions. */
@Mixin(Wolf.class)
public abstract class WolfDimensionsMixin extends TamableAnimal {
    protected WolfDimensionsMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BabyDimensions.WOLF : super.getDefaultDimensions(pose);
    }
}
