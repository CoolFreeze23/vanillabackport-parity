package com.coolfreeze23.vbparity.garden.client;

import com.blackgear.vanillabackport.common.registries.worldgen.ModBiomes;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.phys.Vec3;

/**
 * GARDEN-20: native audio/music_volume and audio/background_music for the pale garden, computed the way 26.2's
 * EnvironmentAttributeProbe does it. GaussianSampler visits 6x6x6 quart cells around the camera (kernel 1-4-6-4-1,
 * order z, x, y) and SpatialAttributeInterpolator sums the weights per source in first-seen order (zero weights too).
 * The sources are then folded in that order: music_volume with a float lerp by weight / accumulated weight (pale garden
 * 0, every other biome the default 1), background_music with a step at 0.5 (a source replaces the running value when
 * it holds at least half of the weight accumulated so far). Biomes stand in for native's attribute maps, one per biome.
 * Screens with their own music keep full volume (Minecraft.getMusicVolume).
 */
public final class GardenMusic {
    private static final double[] KERNEL = {0.0, 1.0, 4.0, 6.0, 4.0, 1.0, 0.0};

    private GardenMusic() {
    }

    public static boolean screenHasMusic(Minecraft minecraft) {
        return minecraft.screen != null && minecraft.screen.getBackgroundMusic() != null;
    }

    public static float targetVolume(Minecraft minecraft) {
        if (screenHasMusic(minecraft)) {
            return 1.0F;
        }
        Map<Holder<Biome>, Double> weights = sample(minecraft);
        if (weights.isEmpty()) {
            return 1.0F;
        }
        Float result = null;
        double accumulated = 0.0;
        for (Map.Entry<Holder<Biome>, Double> entry : weights.entrySet()) {
            float value = entry.getKey().is(ModBiomes.PALE_GARDEN) ? 0.0F : 1.0F;
            accumulated += entry.getValue();
            if (result == null) {
                result = value;
            } else {
                result = Mth.lerp((float) (entry.getValue() / accumulated), result, value);
            }
        }
        return Mth.clamp(result, 0.0F, 1.0F);
    }

    /** True when the pale garden's empty background_music wins, so no song plays or starts. */
    public static boolean isSilent(Minecraft minecraft) {
        if (screenHasMusic(minecraft)) {
            return false;
        }
        Map<Holder<Biome>, Double> weights = sample(minecraft);
        Holder<Biome> result = null;
        double accumulated = 0.0;
        for (Map.Entry<Holder<Biome>, Double> entry : weights.entrySet()) {
            accumulated += entry.getValue();
            if (result == null || (float) (entry.getValue() / accumulated) >= 0.5F) {
                result = entry.getKey();
            }
        }
        return result != null && result.is(ModBiomes.PALE_GARDEN);
    }

    private static Map<Holder<Biome>, Double> sample(Minecraft minecraft) {
        Map<Holder<Biome>, Double> weights = new LinkedHashMap<>();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return weights;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 position = camera.isInitialized() ? camera.getPosition() : minecraft.player.getEyePosition();
        Vec3 quart = position.scale(0.25).subtract(0.5, 0.5, 0.5);
        int baseX = Mth.floor(quart.x);
        int baseY = Mth.floor(quart.y);
        int baseZ = Mth.floor(quart.z);
        double relX = quart.x - baseX;
        double relY = quart.y - baseY;
        double relZ = quart.z - baseZ;
        BiomeManager biomes = level.getBiomeManager();
        for (int z = 0; z < 6; z++) {
            double weightZ = Mth.lerp(relZ, KERNEL[z + 1], KERNEL[z]);
            for (int x = 0; x < 6; x++) {
                double weightX = Mth.lerp(relX, KERNEL[x + 1], KERNEL[x]);
                for (int y = 0; y < 6; y++) {
                    double weightY = Mth.lerp(relY, KERNEL[y + 1], KERNEL[y]);
                    Holder<Biome> biome = biomes.getNoiseBiomeAtQuart(baseX - 2 + x, baseY - 2 + y, baseZ - 2 + z);
                    weights.merge(biome, weightX * weightY * weightZ, Double::sum);
                }
            }
        }
        return weights;
    }
}
