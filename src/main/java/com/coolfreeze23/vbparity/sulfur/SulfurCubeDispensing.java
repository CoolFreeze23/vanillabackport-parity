package com.coolfreeze23.vbparity.sulfur;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.SulfurCube;
import com.coolfreeze23.vbparity.sulfur.mixin.SulfurCubeInvoker;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;

/** Native SulfurCubeBlockDispenseItemBehavior: a dispenser feeds one swallowable block into a sulfur cube in front of it. */
public final class SulfurCubeDispensing {
    public static final DispenseItemBehavior FEED = new DefaultDispenseItemBehavior() {
        @Override
        protected ItemStack execute(BlockSource source, ItemStack dispensed) {
            return dispenseBlock(source.level(), target(source), dispensed) ? dispensed : super.execute(source, dispensed);
        }
    };
    private static final Map<DispenseItemBehavior, DispenseItemBehavior> TNT_WRAPPERS = new WeakHashMap<>();

    private SulfurCubeDispensing() {
    }

    public static BlockPos target(BlockSource source) {
        return source.pos().relative(source.state().getValue(DispenserBlock.FACING));
    }

    public static boolean dispenseBlock(ServerLevel level, BlockPos pos, ItemStack dispensed) {
        for (SulfurCube cube : level.getEntitiesOfClass(SulfurCube.class, new AABB(pos))) {
            if (((SulfurCubeInvoker) cube).vbp$equipItem(dispensed)) {
                dispensed.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** Native TNT behaviour tries to feed a sulfur cube before priming. */
    public static synchronized DispenseItemBehavior wrapTnt(DispenseItemBehavior original) {
        return TNT_WRAPPERS.computeIfAbsent(original, TntBehavior::new);
    }

    private record TntBehavior(DispenseItemBehavior original) implements DispenseItemBehavior {
        @Override
        public ItemStack dispense(BlockSource source, ItemStack stack) {
            if (dispenseBlock(source.level(), target(source), stack)) {
                // Same feedback as a successful default dispense (sound 1000, smoke animation 2000).
                Direction facing = source.state().getValue(DispenserBlock.FACING);
                source.level().levelEvent(1000, source.pos(), 0);
                source.level().levelEvent(2000, source.pos(), facing.get3DDataValue());
                return stack;
            }
            return this.original.dispense(source, stack);
        }
    }
}
