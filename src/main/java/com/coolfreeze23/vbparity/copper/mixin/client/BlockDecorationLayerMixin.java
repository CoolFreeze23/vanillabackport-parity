package com.coolfreeze23.vbparity.copper.mixin.client;

import com.blackgear.vanillabackport.client.level.layer.BlockDecorationLayer;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.golem.copper_golem.CopperGolem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.function.Function;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** COPPER-25: native renders the antenna block (BODY here, native SADDLE) with the item's block_state component applied. */
@Mixin(BlockDecorationLayer.class)
public abstract class BlockDecorationLayerMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object vbparity$antennaBlockState(Function<Object, Object> blockModel, Object entity, Operation<Object> original) {
        if (entity instanceof CopperGolem golem) {
            ItemStack antenna = golem.getItemBySlot(com.coolfreeze23.vbparity.copper.CopperAntenna.SLOT);
            if (antenna.getItem() instanceof BlockItem block) {
                return antenna.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(block.getBlock().defaultBlockState());
            }
            return null;
        }
        return original.call(blockModel, entity);
    }
}
