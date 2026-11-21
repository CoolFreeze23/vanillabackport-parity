package com.coolfreeze23.vbparity.sulfur.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.AbstractCubeMob;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.sulfur_cube.SulfurCube;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SULFUR-18. Native SulfurCube keeps its swallowed block in EquipmentSlot.BODY (saved as body_armor_item, reachable as
 * armor.body); VB uses CHEST. 1.21.1 has BODY (wolf/horse armour), so every CHEST access in VB's cube becomes BODY.
 * Cubes saved by VB alone (block in CHEST, BODY empty) are moved over on load, drop chance included.
 */
@Mixin(SulfurCube.class)
public abstract class SulfurCubeBodySlotMixin extends AbstractCubeMob {
    protected SulfurCubeBodySlotMixin(EntityType<? extends AgeableMob> type, Level level) {
        super(type, level);
    }

    @ModifyExpressionValue(
        method = {"hasBodyItem", "getContainedBlock", "collectEquipmentChanges", "mobInteract", "equipItem", "shear", "canHoldItem", "pickUpItem"},
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/EquipmentSlot;CHEST:Lnet/minecraft/world/entity/EquipmentSlot;", opcode = Opcodes.GETSTATIC)
    )
    private EquipmentSlot vbp$bodySlot(EquipmentSlot chest) {
        return EquipmentSlot.BODY;
    }

    /** The previous swallowed block is LivingEntity.lastBodyItemStack for BODY (VB reads lastArmorItemStacks[CHEST]). */
    @ModifyExpressionValue(method = "collectEquipmentChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;", ordinal = 0))
    private Object vbp$previousBodyItem(Object previous) {
        return ((LivingEntityBodyAccessor) this).vbp$lastBodyItemStack();
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void vbp$migrateChestBlock(CompoundTag compound, CallbackInfo ci) {
        ItemStack chest = this.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty() && this.getItemBySlot(EquipmentSlot.BODY).isEmpty()) {
            float dropChance = ((MobAccessor) this).vbp$armorDropChances()[EquipmentSlot.CHEST.getIndex()];
            this.setItemSlot(EquipmentSlot.BODY, chest);
            this.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            this.setDropChance(EquipmentSlot.BODY, dropChance);
        }
    }
}
