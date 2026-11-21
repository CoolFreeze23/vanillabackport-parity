package com.coolfreeze23.vbparity.mayhem.mixin;

import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.blackgear.vanillabackport.core.VanillaBackport;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * SPAWN-10: native ZombieHorse.finalizeSpawn creates, finalizes, arms and mounts the zombie rider of a natural spawn
 * first and only then calls super (attribute rolls and the Mob draws). VB did the same at the very end of
 * Mob.finalizeSpawn; its hook is turned off in ZombieHorseMixin and the same body runs here.
 */
@Mixin(AbstractHorse.class)
public abstract class AbstractHorseRiderMixin {
    @Inject(method = "finalizeSpawn", at = @At("HEAD"))
    private void vbp$zombieHorseRider(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data,
                                      CallbackInfoReturnable<SpawnGroupData> cir) {
        if (!((Object) this instanceof ZombieHorse horse) || reason != MobSpawnType.NATURAL) {
            return;
        }

        Zombie zombie = EntityType.ZOMBIE.create(horse.level());
        if (zombie != null) {
            zombie.moveTo(horse.getX(), horse.getY(), horse.getZ(), horse.getYRot(), 0.0F);
            zombie.finalizeSpawn(level, difficulty, reason, null);
            zombie.setItemSlot(EquipmentSlot.MAINHAND,
                new ItemStack(VanillaBackport.COMMON_CONFIG.hasSpears.get() ? ModItems.IRON_SPEAR.get() : Items.IRON_SWORD));
            zombie.startRiding(horse, false);
        }
    }
}
