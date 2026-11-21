package com.coolfreeze23.vbparity.ghast.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.camel.CamelHusk;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.skeleton.Parched;
import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.blackgear.vanillabackport.core.VanillaBackport;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GHAST-43: the current game's Husk.finalizeSpawn tail, run when a husk finishes Zombie.finalizeSpawn (1.21.1 Husk
 * has no finalizeSpawn of its own). By now Zombie has made the group data (so the first husk of a pack rolls too) and
 * handed out its equipment (so the spear is not replaced). The native husk also makes its own pick-up-loot roll
 * before the camel husk check; that roll is kept so the random sequence matches.
 */
@Mixin(Zombie.class)
public abstract class ZombieJockeyMixin extends Monster {
    protected ZombieJockeyMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Inject(method = "finalizeSpawn", at = @At("RETURN"), cancellable = true)
    private void vbp$camelHuskJockey(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData incoming,
                                     CallbackInfoReturnable<SpawnGroupData> cir) {
        if (!((Object) this instanceof Husk husk)) {
            return;
        }

        RandomSource random = level.getRandom();
        SpawnGroupData data = cir.getReturnValue();
        if (reason != MobSpawnType.CONVERSION) {
            this.setCanPickUpLoot(random.nextFloat() < 0.55F * difficulty.getSpecialMultiplier());
        }

        if (data instanceof Zombie.ZombieGroupData zombieData) {
            CamelHusk.HuskGroupData huskData = new CamelHusk.HuskGroupData(zombieData);
            huskData.triedToSpawnCamelHusk = reason != MobSpawnType.NATURAL;
            data = huskData;
        }

        if (data instanceof CamelHusk.HuskGroupData huskData && !huskData.triedToSpawnCamelHusk) {
            BlockPos pos = husk.blockPosition();
            if (level.noCollision(ModEntityTypes.CAMEL_HUSK.get().getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
                huskData.triedToSpawnCamelHusk = true;
                if (random.nextFloat() < 0.1F) {
                    Item heldItem = VanillaBackport.COMMON_CONFIG.hasSpears.get() ? ModItems.IRON_SPEAR.get() : Items.IRON_SWORD;
                    this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(heldItem));
                    CamelHusk camel = ModEntityTypes.CAMEL_HUSK.get().create(husk.level());
                    if (camel != null) {
                        camel.setPos(husk.getX(), husk.getY(), husk.getZ());
                        camel.finalizeSpawn(level, difficulty, reason, null);
                        husk.startRiding(camel, true);
                        level.addFreshEntity(camel);
                        Parched parched = ModEntityTypes.PARCHED.get().create(husk.level());
                        if (parched != null) {
                            parched.moveTo(husk.getX(), husk.getY(), husk.getZ(), husk.getYRot(), 0.0F);
                            parched.finalizeSpawn(level, difficulty, reason, null);
                            parched.startRiding(camel, false);
                            level.addFreshEntityWithPassengers(parched);
                        }
                    }
                }
            }
        }

        cir.setReturnValue(data);
    }
}
