package com.coolfreeze23.vbparity.garden;

import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Native bee_food and bee_attractive list only the open eyeblossom. In 1.21.1 bees use #flowers for both, and VB's
 * small_flowers adds the closed eyeblossom too, so the closed one is excluded here.
 */
public final class GardenBees {
    private GardenBees() {
    }

    public static boolean isClosedEyeblossom(ItemStack stack) {
        return stack.is(ModBlocks.CLOSED_EYEBLOSSOM.get().asItem());
    }

    public static boolean isClosedEyeblossom(BlockState state) {
        return state.is(ModBlocks.CLOSED_EYEBLOSSOM.get());
    }
}
