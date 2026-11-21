package com.coolfreeze23.vbparity.flora.client;

import com.blackgear.vanillabackport.client.api.modules.falling_leaves.LeafColors;
import com.blackgear.vanillabackport.client.api.modules.leaf_litter.DryLeafColorReloadListener;
import com.coolfreeze23.vbparity.VBParity;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ColorResolver;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = VBParity.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FloraClient {
    /** Native {@code BiomeColors.DRY_FOLIAGE_COLOR_RESOLVER}: the biome's fixed dry-foliage colour, else its colormap colour. */
    public static final ColorResolver DRY_FOLIAGE_COLOR_RESOLVER = (biome, x, z) -> {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            OptionalInt fixed = DryLeafColorReloadListener.getColorForBiome(level.registryAccess().registryOrThrow(Registries.BIOME).wrapAsHolder(biome));
            if (fixed.isPresent()) {
                return fixed.getAsInt();
            }
        }
        return LeafColors.DRY_FOLIAGE_COLOR_RESOLVER.getColor(biome, x, z);
    };

    @SubscribeEvent
    public static void registerColorResolvers(RegisterColorHandlersEvent.ColorResolvers event) {
        event.register(DRY_FOLIAGE_COLOR_RESOLVER);
    }

    private FloraClient() {}
}
