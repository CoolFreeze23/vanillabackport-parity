package com.coolfreeze23.vbparity.ai.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.nautilus.AbstractNautilus;
import com.blackgear.vanillabackport.common.level.items.NautilusArmorItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * AI-MNT-07: native EquipmentDispenseItemBehavior equips nautilus armour on the first living entity in front that
 * canEquipWithDispenser: a nautilus or zombie nautilus that is alive, adult and tame (canUseSlot BODY) with an empty
 * BODY slot (canDispenserEquipIntoSlot BODY). 1.21.1 asks Mob.canTakeItem, which needs canPickUpLoot, so it never did.
 */
@Mixin(ArmorItem.class)
public abstract class NautilusArmorDispenseMixin {
    @Inject(method = "dispenseArmor", at = @At("HEAD"), cancellable = true)
    private static void vbp$nautilusArmor(BlockSource source, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!(stack.getItem() instanceof NautilusArmorItem)) {
            return;
        }
        BlockPos pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
        List<LivingEntity> targets = source.level().getEntitiesOfClass(LivingEntity.class, new AABB(pos), entity ->
            entity.isAlive() && !entity.isSpectator() && entity instanceof AbstractNautilus nautilus
                && nautilus.canUseSlot(EquipmentSlot.BODY) && nautilus.getItemBySlot(EquipmentSlot.BODY).isEmpty());
        if (targets.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }
        AbstractNautilus target = (AbstractNautilus) targets.get(0);
        target.setItemSlot(EquipmentSlot.BODY, stack.split(1));
        target.setDropChance(EquipmentSlot.BODY, 2.0F);
        target.setPersistenceRequired();
        cir.setReturnValue(true);
    }
}
