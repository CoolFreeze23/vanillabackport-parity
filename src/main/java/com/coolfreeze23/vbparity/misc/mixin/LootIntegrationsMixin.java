package com.coolfreeze23.vbparity.misc.mixin;

import com.blackgear.platform.common.data.LootModifier;
import com.blackgear.vanillabackport.common.integrations.LootIntegrations;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Native chests/ruined_portal lodestone pool lists the empty entry (weight 1) before the lodestone (weight 2);
 * entry order decides which random values give which result. The 4th addPool in modify() is the lodestone pool
 * (it only runs when VB's lodestone_loot option is on).
 */
@Mixin(LootIntegrations.class)
public abstract class LootIntegrationsMixin {
    @WrapOperation(method = "modify", at = @At(value = "INVOKE", ordinal = 3,
            target = "Lcom/blackgear/platform/common/data/LootModifier$LootTableContext;addPool(Lnet/minecraft/world/level/storage/loot/LootPool$Builder;)V"))
    private void vbp$nativeLodestonePool(LootModifier.LootTableContext context, LootPool.Builder vbPool, Operation<Void> original) {
        original.call(context, LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(EmptyLootItem.emptyItem().setWeight(1))
                .add(LootItem.lootTableItem(Items.LODESTONE).setWeight(2).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))));
    }
}
