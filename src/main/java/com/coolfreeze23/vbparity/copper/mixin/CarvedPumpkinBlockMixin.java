package com.coolfreeze23.vbparity.copper.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SPAWN-08: native CarvedPumpkinBlock.trySpawnGolem checks the snow golem, then the iron golem, and only then the copper
 * golem. VB checks copper first at HEAD, so a pumpkin touching any copper block built a copper golem instead. This runs
 * at HEAD before VB's hook (lower priority applies first): when a snow or iron golem pattern matches it builds that golem
 * exactly as the 1.21.1 body does and returns, so VB's copper check never sees the pumpkin. Otherwise nothing changes.
 */
@Mixin(value = CarvedPumpkinBlock.class, priority = 900)
public abstract class CarvedPumpkinBlockMixin {
    @Shadow
    private BlockPattern getOrCreateSnowGolemFull() {
        throw new AssertionError();
    }

    @Shadow
    private BlockPattern getOrCreateIronGolemFull() {
        throw new AssertionError();
    }

    @Shadow
    private static void spawnGolemInWorld(Level level, BlockPattern.BlockPatternMatch match, Entity golem, BlockPos pos) {
        throw new AssertionError();
    }

    @Inject(method = "trySpawnGolem", at = @At("HEAD"), cancellable = true)
    private void vbparity$copper$snowAndIronFirst(Level level, BlockPos pos, CallbackInfo ci) {
        BlockPattern.BlockPatternMatch snow = this.getOrCreateSnowGolemFull().find(level, pos);
        if (snow != null) {
            SnowGolem golem = EntityType.SNOW_GOLEM.create(level);
            if (golem != null) {
                spawnGolemInWorld(level, snow, golem, snow.getBlock(0, 2, 0).getPos());
            }
            ci.cancel();
            return;
        }
        BlockPattern.BlockPatternMatch iron = this.getOrCreateIronGolemFull().find(level, pos);
        if (iron != null) {
            IronGolem golem = EntityType.IRON_GOLEM.create(level);
            if (golem != null) {
                golem.setPlayerCreated(true);
                spawnGolemInWorld(level, iron, golem, iron.getBlock(1, 2, 0).getPos());
            }
            ci.cancel();
        }
    }
}
