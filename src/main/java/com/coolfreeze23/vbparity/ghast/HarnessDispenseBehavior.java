package com.coolfreeze23.vbparity.ghast;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.happy_ghast.HappyGhast;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;

/**
 * Dispensers put a harness on an adult, unharnessed happy ghast in front of them, as the current game's
 * equipment dispense behaviour does for any dispensable equippable item. With no ghast there, the harness is
 * dropped like any other item.
 */
public final class HarnessDispenseBehavior extends DefaultDispenseItemBehavior {
    public static final HarnessDispenseBehavior INSTANCE = new HarnessDispenseBehavior();

    @Override
    protected ItemStack execute(BlockSource source, ItemStack stack) {
        return dispenseHarness(source, stack) ? stack : super.execute(source, stack);
    }

    public static boolean dispenseHarness(BlockSource source, ItemStack stack) {
        BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        List<HappyGhast> ghasts = source.level().getEntitiesOfClass(
            HappyGhast.class, new AABB(pos),
            ghast -> ghast.isAlive() && !ghast.isSpectator() && ghast.canBeHarnessed() && ghast.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
        );
        if (ghasts.isEmpty()) {
            return false;
        }

        HappyGhast ghast = ghasts.get(0);
        ghast.setItemSlot(EquipmentSlot.CHEST, stack.split(1));
        ghast.setGuaranteedDrop(EquipmentSlot.CHEST);
        ghast.setPersistenceRequired();
        return true;
    }
}
