package com.coolfreeze23.vbparity.variants.mixin;

import com.blackgear.vanillabackport.core.VanillaBackport;
import com.coolfreeze23.vbparity.variants.BabyDimensions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * VARIANTS-13: native lamb dimensions.
 * VARIANTS-20: while farm-animal variants are on, VanillaBackport replaces the spawn colour with its biome table but
 * still rolls the legacy colour first (1-2 wasted draws). Native rolls only the table, so inside {@code finalizeSpawn}
 * the legacy roll returns its (unused) result without touching the random.
 */
@Mixin(Sheep.class)
public abstract class SheepMixin extends Animal {
    @Unique
    private static final ThreadLocal<Boolean> VBP_IN_SPAWN = ThreadLocal.withInitial(() -> false);

    protected SheepMixin(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isBaby() ? BabyDimensions.SHEEP : super.getDefaultDimensions(pose);
    }

    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void vbp$enterSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, SpawnGroupData data,
                                CallbackInfoReturnable<SpawnGroupData> cir) {
        VBP_IN_SPAWN.set(true);
    }

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void vbp$leaveSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, SpawnGroupData data,
                                CallbackInfoReturnable<SpawnGroupData> cir) {
        VBP_IN_SPAWN.set(false);
    }

    @Inject(method = "getRandomSheepColor", at = @At("HEAD"), cancellable = true)
    private static void vbp$skipLegacyRoll(RandomSource random, CallbackInfoReturnable<DyeColor> cir) {
        if (VBP_IN_SPAWN.get() && VanillaBackport.COMMON_CONFIG.hasFarmAnimalVariants.get()) {
            cir.setReturnValue(DyeColor.WHITE);
        }
    }
}
