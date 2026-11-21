package com.coolfreeze23.vbparity.variants.mixin;

import com.blackgear.vanillabackport.common.api.extensions.access.entity.MobBehaviorAccess;
import com.coolfreeze23.vbparity.ai.VariantOrder;
import com.coolfreeze23.vbparity.variants.SoundVariants;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * VARIANTS-03..05 / AI-VAR-01 / SPAWN-11: a spawned cow, pig or chicken picks its variant (VanillaBackport's pick) and
 * then its sound variant first thing in finalizeSpawn, as native Cow/Pig/Chicken.finalizeSpawn do before calling
 * super (the AgeableMob baby roll and the Mob follow-range and handedness rolls). VB's own late pick at Mob.finalizeSpawn
 * RETURN is switched off for these (VariantLatePickMixin). Mooshrooms pick neither. Cats pick in {@link CatSpawnMixin}.
 */
@Mixin(AgeableMob.class)
public abstract class AgeableMobMixin extends PathfinderMob {
    protected AgeableMobMixin(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void vbp$pickVariantsFirst(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, SpawnGroupData data,
                                       CallbackInfoReturnable<SpawnGroupData> cir) {
        EntityType<?> t = this.getType();
        if (t != EntityType.COW && t != EntityType.PIG && t != EntityType.CHICKEN) {
            return;
        }
        if (this instanceof MobBehaviorAccess access) {
            VariantOrder.runEarly(() -> access.vb$finalizeSpawn(level, difficulty, type, data));
        }
        SoundVariants.Family<?> family = SoundVariants.familyOf(this);
        if (family != null) {
            family.pickRandom(this, level.getRandom());
        }
    }
}
