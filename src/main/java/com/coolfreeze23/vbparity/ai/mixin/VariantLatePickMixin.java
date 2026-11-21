package com.coolfreeze23.vbparity.ai.mixin;

import com.coolfreeze23.vbparity.ai.VariantOrder;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AI-VAR-01 / AI-VAR-02 / SPAWN-11: VB's cow, pig and chicken variant pick only runs from the early call in
 * AgeableMob.finalizeSpawn (variants AgeableMobMixin), not again at Mob.finalizeSpawn RETURN. Mooshrooms (a Cow
 * subclass in 1.21.1, an AbstractCow natively) never pick a cow variant. Applied after VB's mixins (priority).
 */
@Mixin(value = {Cow.class, Pig.class, Chicken.class}, priority = 1500)
public abstract class VariantLatePickMixin {
    @Inject(method = "vb$finalizeSpawn", at = @At("HEAD"), cancellable = true, remap = false)
    private void vbp$onlyEarly(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data,
                               CallbackInfoReturnable<SpawnGroupData> cir) {
        if (!VariantOrder.early()) {
            cir.setReturnValue(data);
        }
    }
}
