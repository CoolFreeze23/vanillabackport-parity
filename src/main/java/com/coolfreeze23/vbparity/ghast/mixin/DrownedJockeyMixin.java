package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.ZombieNautilus;
import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.blackgear.vanillabackport.core.VanillaBackport;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GHAST-42: zombie nautilus jockeys. VB rolls for one from Mob.finalizeSpawn's return, which runs inside the super
 * call of Zombie.finalizeSpawn, before Zombie gives the drowned its trident, so the trident check never passes for
 * a natural spawn; it also draws the random float before the other checks. The current game checks at the end of
 * Drowned.finalizeSpawn (after the trident and the nautilus shell roll), in the order reason, trident, random, baby,
 * biome. VB's hook is switched off here (this mixin applies after VB's, which added vb$finalizeSpawn to Drowned) and
 * the roll is made at the native point, with VB's has_nautilus and zombie_nautilus_spawn_chance options.
 */
@Mixin(value = Drowned.class, priority = 1500)
public abstract class DrownedJockeyMixin extends Zombie {
    protected DrownedJockeyMixin(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Inject(method = "vb$finalizeSpawn", at = @At("HEAD"), cancellable = true, remap = false)
    private void vbp$skipEarlyJockeyRoll(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data,
                                         CallbackInfoReturnable<SpawnGroupData> cir) {
        cir.setReturnValue(data);
    }

    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void vbp$nautilusJockey(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data,
                                    CallbackInfoReturnable<SpawnGroupData> cir) {
        if (VanillaBackport.COMMON_CONFIG.hasNautilus.get()
            && (reason == MobSpawnType.NATURAL || reason == MobSpawnType.STRUCTURE)
            && this.getMainHandItem().is(Items.TRIDENT)
            && level.getRandom().nextFloat() < VanillaBackport.COMMON_CONFIG.zombieNautilusSpawnChance.get()
            && !this.isBaby()
            && !level.getBiome(this.blockPosition()).is(BiomeTags.MORE_FREQUENT_DROWNED_SPAWNS)) {
            ZombieNautilus nautilus = ModEntityTypes.ZOMBIE_NAUTILUS.get().create(this.level());
            if (nautilus != null) {
                if (reason == MobSpawnType.STRUCTURE) {
                    nautilus.setPersistenceRequired();
                }

                nautilus.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                nautilus.finalizeSpawn(level, difficulty, reason, null);
                this.startRiding(nautilus, false);
                level.addFreshEntity(nautilus);
            }
        }
    }
}
