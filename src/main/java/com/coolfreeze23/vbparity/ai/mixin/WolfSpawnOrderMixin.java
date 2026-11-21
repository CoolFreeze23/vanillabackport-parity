package com.coolfreeze23.vbparity.ai.mixin;

import com.blackgear.vanillabackport.common.api.modules.sound_variant.SoundVariantHolder;
import com.blackgear.vanillabackport.common.api.modules.sound_variant.WolfSoundVariant;
import com.blackgear.vanillabackport.common.api.modules.sound_variant.WolfSoundVariants;
import com.coolfreeze23.vbparity.ai.NativeSoundOrder;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AI-VAR-01 / AI-VAR-03 (wolf): native Wolf.finalizeSpawn draws the variant (nextInt(1) over the one matching entry,
 * outside a pack only), then the sound variant (nextInt(7), id order), then calls super. 1.21.1 draws no variant and
 * VB picks the sound after super. The VB late pick reuses this value without a draw (WolfLateSoundMixin).
 */
@Mixin(Wolf.class)
public abstract class WolfSpawnOrderMixin {
    @Inject(method = "finalizeSpawn", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/animal/WolfVariants;getSpawnVariant(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/core/Holder;)Lnet/minecraft/core/Holder;",
        shift = At.Shift.AFTER))
    private void vbp$variantDraw(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, SpawnGroupData data,
                                 CallbackInfoReturnable<SpawnGroupData> cir) {
        level.getRandom().nextInt(1);
    }

    @Inject(method = "finalizeSpawn", at = @At(value = "INVOKE",
        target = "finalizeSpawn(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/world/entity/SpawnGroupData;)Lnet/minecraft/world/entity/SpawnGroupData;"))
    @SuppressWarnings("unchecked")
    private void vbp$soundBeforeSuper(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, SpawnGroupData data,
                                      CallbackInfoReturnable<SpawnGroupData> cir) {
        ((SoundVariantHolder<WolfSoundVariant>) (Object) this).vb$setSoundVariant(NativeSoundOrder.pick(WolfSoundVariants.REGISTRIES, level.getRandom()));
    }
}
