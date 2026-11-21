package com.coolfreeze23.vbparity.data;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = "vbparity", bus = EventBusSubscriber.Bus.MOD)
public final class DataRegistration {
    private DataRegistration() {
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
            helper -> helper.register(ResourceLocation.fromNamespaceAndPath("vbparity", "data_spawn_parity"), DataSpawnParity.CODEC));
    }
}
