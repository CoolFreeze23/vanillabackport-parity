package com.coolfreeze23.vbparity.sulfur;

import com.blackgear.vanillabackport.common.level.block_entities.PotentSulfurBlockEntity;
import com.blackgear.vanillabackport.core.data.tags.ModEntityTypeTags;
import com.coolfreeze23.vbparity.sulfur.mixin.PotentSulfurBlockEntityInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Client half of the native geyser launch (PotentSulfurBlock#getTicker runs LAUNCH_ENTITY_TICKER on the client too):
 * the client predicts the launch for entities it simulates itself, i.e. the local player and the vehicle it drives.
 */
public final class GeyserLaunch {
    public static final BlockEntityTicker<PotentSulfurBlockEntity> CLIENT_LAUNCH_TICKER = (level, pos, state, sulfur) -> {
        BlockPos source = PotentSulfurBlockEntityInvoker.vbp$findNoxiousGasSourceBlock(level, pos);
        if (source == null) {
            return;
        }
        int waterBlocks = source.getY() - pos.getY() - 1;
        int geyserForceHeight = PotentSulfurBlockEntityInvoker.vbp$getUnobstructedBlockCount(level, pos.above(), waterBlocks);
        AABB aabb = new AABB(pos.above()).expandTowards(0.0, geyserForceHeight - 1, 0.0);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, aabb, EntitySelector.NO_SPECTATORS.and(EntitySelector.ENTITY_STILL_ALIVE))) {
            Vec3 velocity = entity.getDeltaMovement();
            entity.checkSlowFallDistance();
            if (entity.isControlledByLocalInstance()
                && !(entity instanceof Player player && player.getAbilities().flying)
                && !entity.isPassenger()
                && !entity.getType().is(ModEntityTypeTags.NOT_AFFECTED_BY_GEYSERS)
                && velocity.y < 0.3F + waterBlocks * 0.1) {
                entity.addDeltaMovement(new Vec3(0.0, 0.2F, 0.0));
            }
        }
    };

    private GeyserLaunch() {
    }
}
