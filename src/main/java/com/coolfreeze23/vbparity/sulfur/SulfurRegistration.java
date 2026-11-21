package com.coolfreeze23.vbparity.sulfur;

import com.coolfreeze23.vbparity.VBParity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = VBParity.MOD_ID, bus = EventBusSubscriber.Bus.MOD) // mod-bus RegisterEvent
public final class SulfurRegistration {
    private SulfurRegistration() {
    }

    /**
     * Vanilla builds the overworld biome table once during bootstrap, before mods and configs load (VB's Platform listener
     * adds nothing then either), so the native sulfur-caves point is only emitted from common setup on.
     */
    private static volatile boolean sulfurCavesPlacement;

    public static boolean sulfurCavesPlacement() {
        return sulfurCavesPlacement;
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> sulfurCavesPlacement = com.blackgear.vanillabackport.core.VanillaBackport.COMMON_CONFIG.hasSulfurCaves.get());
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, helper -> {
            for (SoundEvent sound : SulfurSounds.ALL) {
                helper.register(sound.getLocation(), sound);
            }
        });
        event.register(
            NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
            helper -> helper.register(ResourceLocation.fromNamespaceAndPath(VBParity.MOD_ID, "sulfur_caves_order"), SulfurCavesOrderModifier.CODEC)
        );
    }
}
