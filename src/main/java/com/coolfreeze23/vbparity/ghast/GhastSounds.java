package com.coolfreeze23.vbparity.ghast;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/** The lead and shears sound events the current game uses for leads, knots and leash shearing. */
@EventBusSubscriber(modid = "vbparity", bus = EventBusSubscriber.Bus.MOD)
public final class GhastSounds {
    public static final SoundEvent LEAD_TIED = event("item.lead.tied");
    public static final SoundEvent LEAD_UNTIED = event("item.lead.untied");
    public static final SoundEvent LEAD_BREAK = event("item.lead.break");
    public static final SoundEvent SHEARS_SNIP = event("item.shears.snip");

    private GhastSounds() {
    }

    private static SoundEvent event(String name) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.withDefaultNamespace(name));
    }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, helper -> {
            for (SoundEvent sound : new SoundEvent[]{LEAD_TIED, LEAD_UNTIED, LEAD_BREAK, SHEARS_SNIP}) {
                helper.register(sound.getLocation(), sound);
            }
        });
    }
}
