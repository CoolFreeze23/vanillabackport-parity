package com.coolfreeze23.vbparity.ghast.mixin;

import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GHAST-43: switch off VB's camel husk roll, which runs from Mob.finalizeSpawn's return before Zombie has made its
 * group data and equipment. ZombieJockeyMixin makes the roll at the end of the husk's finalizeSpawn instead.
 * This mixin applies after VB's HuskMixin, which added vb$finalizeSpawn to Husk.
 */
@Mixin(value = Husk.class, priority = 1500)
public abstract class HuskJockeyMixin extends Zombie {
    protected HuskJockeyMixin(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Inject(method = "vb$finalizeSpawn", at = @At("HEAD"), cancellable = true, remap = false)
    private void vbp$skipEarlyCamelHuskRoll(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data,
                                            CallbackInfoReturnable<SpawnGroupData> cir) {
        cir.setReturnValue(data);
    }
}
