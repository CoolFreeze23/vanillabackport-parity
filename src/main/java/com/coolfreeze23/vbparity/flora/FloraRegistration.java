package com.coolfreeze23.vbparity.flora;

import com.coolfreeze23.vbparity.VBParity;
import com.coolfreeze23.vbparity.flora.worldgen.ConfigSwitchFeature;
import com.coolfreeze23.vbparity.flora.worldgen.VegetalOrderModifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = VBParity.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class FloraRegistration {
    public static final ConfigSwitchFeature CONFIG_SWITCH = new ConfigSwitchFeature();

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.FEATURE, helper -> helper.register(ResourceLocation.fromNamespaceAndPath(VBParity.MOD_ID, "config_switch"), CONFIG_SWITCH));
        event.register(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
            helper -> helper.register(ResourceLocation.fromNamespaceAndPath(VBParity.MOD_ID, "flora_vegetal_order"), VegetalOrderModifier.CODEC));
    }

    private FloraRegistration() {}
}
