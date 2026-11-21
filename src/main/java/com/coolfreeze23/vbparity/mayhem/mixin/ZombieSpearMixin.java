package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.blackgear.vanillabackport.core.VanillaBackport;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * MAYHEM-07: VB rolls nextInt(6) and gives the iron spear on a 1, but the vanilla else-branch then replaces it with
 * an iron shovel. Natively a 1 keeps the spear (sword 1/6, spear 1/6, shovel 4/6). The shovel call is swapped for
 * the spear on a 1.
 */
@Mixin(value = Zombie.class, priority = 1500)
public abstract class ZombieSpearMixin {
    @WrapOperation(
        method = "populateDefaultEquipmentSlots",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Zombie;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V", ordinal = 1)
    )
    private void vbp$keepSpear(Zombie zombie, EquipmentSlot slot, ItemStack stack, Operation<Void> original, @Local(ordinal = 0) int roll) {
        if (roll == 1 && VanillaBackport.COMMON_CONFIG.hasSpears.get() && VBP$CONVERSION_DEPTH.get()[0] == 0) {
            original.call(zombie, slot, new ItemStack(ModItems.IRON_SPEAR.get()));
        } else {
            original.call(zombie, slot, stack);
        }
    }

    /**
     * VB widens the weapon roll from nextInt(3) to nextInt(6) even with has_spears off, which made swords 1/6 instead
     * of the 1.21.1 1/3. With spears off the roll is the unmodified one again.
     */
    @WrapOperation(method = "populateDefaultEquipmentSlots", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int vbp$vanillaRollWithoutSpears(RandomSource random, int bound, Operation<Integer> original) {
        boolean vanillaRoll = !VanillaBackport.COMMON_CONFIG.hasSpears.get() || VBP$CONVERSION_DEPTH.get()[0] > 0;
        return original.call(random, bound == 6 && vanillaRoll ? 3 : bound);
    }

    /**
     * SPAWN-13 (spear part): native conversions (villager to zombie villager) never roll equipment. The general 1.21.1
     * conversion gear is vanilla and left alone, but VB's spear and its widened roll stay out of conversions.
     */
    @Unique
    private static final ThreadLocal<int[]> VBP$CONVERSION_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void vbp$enterConversion(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data,
                                     CallbackInfoReturnable<SpawnGroupData> cir) {
        if (reason == MobSpawnType.CONVERSION) {
            VBP$CONVERSION_DEPTH.get()[0]++;
        }
    }

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void vbp$leaveConversion(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data,
                                     CallbackInfoReturnable<SpawnGroupData> cir) {
        if (reason == MobSpawnType.CONVERSION && VBP$CONVERSION_DEPTH.get()[0] > 0) {
            VBP$CONVERSION_DEPTH.get()[0]--;
        }
    }

    /**
     * SPAWN-14: native Zombie.finalizeSpawn always draws the door-breaking float; 1.21.1 skips it for zombies that
     * cannot break doors (drowned, zombified piglins), which shifts their trident, shell, spear and jockey rolls.
     */
    @WrapOperation(method = "finalizeSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Zombie;supportsBreakDoorGoal()Z"))
    private boolean vbp$alwaysDrawDoorRoll(Zombie zombie, Operation<Boolean> original, @Local RandomSource random) {
        boolean supports = original.call(zombie);
        if (!supports) {
            random.nextFloat();
        }
        return supports;
    }
}
