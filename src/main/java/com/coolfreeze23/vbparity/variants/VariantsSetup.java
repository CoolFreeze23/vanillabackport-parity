package com.coolfreeze23.vbparity.variants;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = "vbparity")
public final class VariantsSetup {
    private VariantsSetup() {
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, helper -> {
            for (SoundEvent sound : VariantSounds.all()) {
                helper.register(sound.getLocation(), sound);
            }
            // The synced-data keys must exist before the first entity data packet, on both sides.
            SoundVariants.init();
        });
    }
}
