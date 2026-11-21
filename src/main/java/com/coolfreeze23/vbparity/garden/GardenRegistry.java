package com.coolfreeze23.vbparity.garden;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Garden registrations: the {@code minecraft:block_crumble} particle type (native 1.21.4+) used by the creaking's
 * tear-down, plus common-setup property fixes.
 */
@EventBusSubscriber(modid = "vbparity", bus = EventBusSubscriber.Bus.MOD)
public final class GardenRegistry {
    public static final ResourceLocation BLOCK_CRUMBLE_ID = ResourceLocation.withDefaultNamespace("block_crumble");

    public static final ParticleType<BlockParticleOption> BLOCK_CRUMBLE = new ParticleType<BlockParticleOption>(false) {
        @Override
        public MapCodec<BlockParticleOption> codec() {
            return BlockParticleOption.codec(this);
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, BlockParticleOption> streamCodec() {
            return BlockParticleOption.streamCodec(this);
        }
    };

    private GardenRegistry() {
    }

    @SubscribeEvent
    static void onRegister(RegisterEvent event) {
        event.register(Registries.PARTICLE_TYPE, helper -> helper.register(BLOCK_CRUMBLE_ID, BLOCK_CRUMBLE));
    }

    @SubscribeEvent
    static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            GardenBlockProperties.apply();
            GardenWorldgen.onCommonSetup();
        });
    }
}
