package com.coolfreeze23.vbparity.copper.mixin;

import com.blackgear.vanillabackport.common.level.entities.mob.animal.golem.copper_golem.CopperGolem;
import com.coolfreeze23.vbparity.copper.CopperAntenna;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COPPER-25: the antenna lives in BODY (native SADDLE), not HEAD, so a golem can wear a head item as natively.
 * VB saves have the antenna in HEAD; on the first load it is moved to BODY with its drop chance, and the save is marked.
 */
@Mixin(CopperGolem.class)
public abstract class CopperGolemAntennaMixin extends AbstractGolem {
    protected CopperGolemAntennaMixin(EntityType<? extends AbstractGolem> type, Level level) {
        super(type, level);
    }

    @ModifyExpressionValue(
        method = {"shear", "readyForShearing"},
        at = @At(
            value = "FIELD",
            target = "Lcom/blackgear/vanillabackport/common/level/entities/mob/animal/golem/copper_golem/CopperGolem;EQUIPMENT_SLOT_ANTENNA:Lnet/minecraft/world/entity/EquipmentSlot;"
        )
    )
    private EquipmentSlot vbparity$copper$antennaSlot(EquipmentSlot vbSlot) {
        return CopperAntenna.SLOT;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void vbparity$copper$markAntennaSlot(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean(CopperAntenna.SAVE_MARKER, true);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void vbparity$copper$migrateAntenna(CompoundTag tag, CallbackInfo ci) {
        if (tag.getBoolean(CopperAntenna.SAVE_MARKER)) {
            return;
        }
        ItemStack head = this.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.isEmpty() && this.getItemBySlot(CopperAntenna.SLOT).isEmpty()) {
            float chance = this.getEquipmentDropChance(EquipmentSlot.HEAD);
            this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            this.setItemSlot(CopperAntenna.SLOT, head);
            this.setDropChance(CopperAntenna.SLOT, chance);
            this.setDropChance(EquipmentSlot.HEAD, 0.085F);
        }
    }
}
