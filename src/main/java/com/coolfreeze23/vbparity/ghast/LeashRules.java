package com.coolfreeze23.vbparity.ghast;

import com.blackgear.vanillabackport.common.api.modules.leash_behavior.LeashPhysics;
import com.blackgear.vanillabackport.core.ModChecker;
import com.blackgear.vanillabackport.core.VanillaBackport;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/** Shared helpers for the leash fixes. All of them follow VB's modern_leashes option. */
public final class LeashRules {
    private LeashRules() {
    }

    public static boolean modernLeashes() {
        return VanillaBackport.COMMON_CONFIG.hasModernLeashes.get() && !ModChecker.SABLE;
    }

    /** The current game's Entity.canInteractWithLevel. */
    public static boolean canInteractWithLevel(Entity entity) {
        return entity.isAlive() && !entity.isRemoved() && !entity.isSpectator();
    }

    /** The current game's LeashFenceKnotEntity.notifyLeasheeRemoved: a knot with nothing tied to it goes away. */
    public static void notifyLeasheeRemoved(@Nullable Entity oldHolder, @Nullable Entity newHolder) {
        if (oldHolder instanceof LeashFenceKnotEntity knot && oldHolder != newHolder && !knot.level().isClientSide() && !knot.isRemoved()
            && LeashPhysics.leashableLeashedTo(knot).isEmpty()) {
            knot.discard();
        }
    }

    /** The current game's LeashFenceKnotEntity.getKnot: the knot on this fence, if there is one. */
    @Nullable
    public static LeashFenceKnotEntity getKnot(Level level, BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        List<LeashFenceKnotEntity> knots = level.getEntitiesOfClass(LeashFenceKnotEntity.class, new AABB(x - 1.0, y - 1.0, z - 1.0, x + 1.0, y + 1.0, z + 1.0));
        for (LeashFenceKnotEntity knot : knots) {
            if (knot.getPos().equals(pos)) {
                return knot;
            }
        }

        return null;
    }
}
