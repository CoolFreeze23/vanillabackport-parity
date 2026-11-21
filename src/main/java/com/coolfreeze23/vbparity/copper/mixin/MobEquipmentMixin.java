package com.coolfreeze23.vbparity.copper.mixin;

import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.blackgear.vanillabackport.core.VanillaBackport;
import com.coolfreeze23.vbparity.copper.CopperEquipment;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * COPPER-19: native armor roll. Tier = nextInt(3) plus three 0.1087 bumps, slots filled HEAD, CHEST, LEGS, FEET, and
 * copper is its own tier (0 leather, 1 copper, 2 gold, 3 chain, 4 iron, 5 diamond). Replaces VB's per-UUID 40% swap
 * of gold for copper (VB's SpawnEquipmentRegistry returns nothing during the roll, see CopperEquipment). The 1.21.1
 * body still runs, patched in place, so other mods' hooks on populateDefaultEquipmentSlots and getEquipmentForSlot
 * keep working. Gated on has_copper_toolset; with it off the 1.21.1 roll runs unchanged.
 */
@Mixin(Mob.class)
public abstract class MobEquipmentMixin {
    @Unique
    private static final EquipmentSlot[] VBPARITY$NATIVE_ORDER = {
        EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.BODY
    };

    @Unique
    private static boolean vbparity$nativeRoll() {
        return VanillaBackport.COMMON_CONFIG.hasCopperToolSet.get();
    }

    @Inject(method = "populateDefaultEquipmentSlots", at = @At("HEAD"))
    private void vbparity$enterRoll(RandomSource random, DifficultyInstance difficulty, CallbackInfo ci) {
        CopperEquipment.enter();
    }

    @Inject(method = "populateDefaultEquipmentSlots", at = @At("RETURN"))
    private void vbparity$exitRoll(RandomSource random, DifficultyInstance difficulty, CallbackInfo ci) {
        CopperEquipment.exit();
    }

    @ModifyArg(method = "populateDefaultEquipmentSlots", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int vbparity$threeBaseTiers(int bound) {
        return vbparity$nativeRoll() && bound == 2 ? 3 : bound;
    }

    @ModifyConstant(method = "populateDefaultEquipmentSlots", constant = @Constant(floatValue = 0.095F))
    private float vbparity$tierBump(float chance) {
        return vbparity$nativeRoll() ? 0.1087F : chance;
    }

    @ModifyExpressionValue(method = "populateDefaultEquipmentSlots", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EquipmentSlot;values()[Lnet/minecraft/world/entity/EquipmentSlot;"))
    private EquipmentSlot[] vbparity$headFirst(EquipmentSlot[] slots) {
        return vbparity$nativeRoll() ? VBPARITY$NATIVE_ORDER.clone() : slots;
    }

    @WrapOperation(
        method = "populateDefaultEquipmentSlots",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;getEquipmentForSlot(Lnet/minecraft/world/entity/EquipmentSlot;I)Lnet/minecraft/world/item/Item;")
    )
    private Item vbparity$copperTier(EquipmentSlot slot, int type, Operation<Item> original) {
        if (!vbparity$nativeRoll()) {
            return original.call(slot, type);
        }
        if (type == 1) {
            return switch (slot) {
                case HEAD -> ModItems.COPPER_HELMET.get();
                case CHEST -> ModItems.COPPER_CHESTPLATE.get();
                case LEGS -> ModItems.COPPER_LEGGINGS.get();
                case FEET -> ModItems.COPPER_BOOTS.get();
                default -> original.call(slot, type);
            };
        }
        return original.call(slot, type >= 2 ? type - 1 : type);
    }
}
