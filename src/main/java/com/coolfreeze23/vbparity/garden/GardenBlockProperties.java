package com.coolfreeze23.vbparity.garden;

import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.coolfreeze23.vbparity.garden.mixin.BlockBehaviourAccessor;
import com.coolfreeze23.vbparity.garden.mixin.BlockPropertiesAccessor;
import com.coolfreeze23.vbparity.garden.mixin.BlockStateBaseAccessor;
import java.util.List;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/** Block property values that VB registers differently from native. */
public final class GardenBlockProperties {
    private GardenBlockProperties() {
    }

    /**
     * Native map colours: pale oak leaves and sapling are METAL (Blocks.java PALE_OAK_LEAVES / PALE_OAK_SAPLING),
     * the closed eyeblossom uses PALE_OAK_LEAVES.defaultMapColor(), which is METAL too.
     * Block states cache their map colour at construction, so every state is updated as well as the properties.
     */
    public static void apply() {
        for (Block block : List.of(ModBlocks.PALE_OAK_LEAVES.get(), ModBlocks.PALE_OAK_SAPLING.get(), ModBlocks.CLOSED_EYEBLOSSOM.get())) {
            ((BlockPropertiesAccessor) ((BlockBehaviourAccessor) block).vbparity$garden$getProperties()).vbparity$garden$setMapColor(state -> MapColor.METAL);
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ((BlockStateBaseAccessor) state).vbparity$garden$setMapColor(MapColor.METAL);
            }
        }
    }
}
