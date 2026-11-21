package com.coolfreeze23.vbparity.variants.mixin;

import com.coolfreeze23.vbparity.variants.SoundVariants;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** VARIANTS-06: a spawned cat picks its sound variant after {@code super.finalizeSpawn} and its breed, as native. */
@Mixin(Cat.class)
public abstract class CatSpawnMixin extends TamableAnimal {
    protected CatSpawnMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void vbp$pickSoundVariant(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, SpawnGroupData data,
                                      CallbackInfoReturnable<SpawnGroupData> cir) {
        SoundVariants.CAT.pickRandom(this, level.getRandom());
    }
}
