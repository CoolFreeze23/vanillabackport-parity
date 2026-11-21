package com.coolfreeze23.vbparity.sulfur;

import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.blackgear.vanillabackport.common.registries.worldgen.ModBiomes;
import com.coolfreeze23.vbparity.VBParity;
import com.coolfreeze23.vbparity.flora.WorldgenProbe;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Sulfur caves worldgen end to end on real overworld-like noise generators. */
@GameTestHolder(VBParity.MOD_ID)
@PrefixGameTestTemplate(false)
public class SulfurWorldgenGameTests {
    private static final String BOX = "sulfur_box";

    /**
     * Native OverworldBiomeBuilder adds the sulfur caves point inside addUndergroundBiomes, after lush caves and before deep
     * dark; there is exactly one. Climate points inside it resolve to sulfur caves.
     */
    @GameTest(template = BOX)
    public static void sulfur_caves_native_climate_place(GameTestHelper h) {
        Climate.ParameterList<Holder<Biome>> list = h.getLevel().registryAccess()
            .registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
            .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD).parameters();
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> values = list.values();
        int lastLush = -1, sulfur = -1, firstDeepDark = -1, sulfurCount = 0;
        for (int i = 0; i < values.size(); i++) {
            Holder<Biome> b = values.get(i).getSecond();
            if (b.is(Biomes.LUSH_CAVES)) {
                lastLush = i;
            } else if (b.is(ModBiomes.SULFUR_CAVES)) {
                sulfur = i;
                sulfurCount++;
            } else if (b.is(Biomes.DEEP_DARK) && firstDeepDark < 0) {
                firstDeepDark = i;
            }
        }
        h.assertTrue(sulfurCount == 1, "one sulfur caves point, found " + sulfurCount);
        h.assertTrue(lastLush < sulfur && sulfur < firstDeepDark && sulfur == lastLush + 1,
            "sulfur caves right after lush caves and before deep dark: lush " + lastLush + " sulfur " + sulfur + " deep dark " + firstDeepDark);
        Climate.ParameterPoint p = values.get(sulfur).getFirst();
        // native: FULL, FULL, span(coast, inland) = [-0.19, 0.55], span(erosion5, erosion6) = [0.45, 1.0], depth [0.2, 0.9], weirdness [-1.1, -0.85], offset 0
        h.assertTrue(p.continentalness().min() == Climate.quantizeCoord(-0.19F) && p.continentalness().max() == Climate.quantizeCoord(0.55F)
                && p.erosion().min() == Climate.quantizeCoord(0.45F) && p.erosion().max() == Climate.quantizeCoord(1.0F)
                && p.depth().min() == Climate.quantizeCoord(0.2F) && p.depth().max() == Climate.quantizeCoord(0.9F)
                && p.weirdness().min() == Climate.quantizeCoord(-1.1F) && p.weirdness().max() == Climate.quantizeCoord(-0.85F) && p.offset() == 0L,
            "native sulfur caves parameters: " + p);
        // inside the box (temperature/humidity mid so dripstone/lush don't win)
        Holder<Biome> inside = list.findValue(Climate.target(0.0F, 0.0F, 0.2F, 0.8F, 0.5F, -1.0F));
        h.assertTrue(inside.is(ModBiomes.SULFUR_CAVES), "climate inside the sulfur box: " + inside.unwrapKey());
        Holder<Biome> outside = list.findValue(Climate.target(0.0F, 0.0F, 0.2F, 0.8F, 0.5F, 0.5F));
        h.assertFalse(outside.is(ModBiomes.SULFUR_CAVES), "climate outside the weirdness band is not sulfur caves");
        // possible-biome order (feature order input): sulfur caves before deep dark
        List<ResourceKey<Biome>> order = new ArrayList<>();
        new WorldgenProbe(h, NoiseGeneratorSettings.OVERWORLD, 0L).biomeSource.possibleBiomes().forEach(b -> order.add(b.unwrapKey().orElseThrow()));
        h.assertTrue(order.indexOf(ModBiomes.SULFUR_CAVES) >= 0 && order.indexOf(ModBiomes.SULFUR_CAVES) < order.indexOf(Biomes.DEEP_DARK),
            "possible biomes: sulfur caves before deep dark: " + order);
        h.succeed();
    }

    /**
     * SULFUR-44/45 end to end: sulfur and cinnabar bands are built in sulfur caves for the overworld, large-biomes and
     * amplified presets (the caves noise settings have no world preset and its router puts no sulfur caves near spawn).
     */
    @GameTest(template = BOX, timeoutTicks = 600)
    public static void sulfur_bands_in_every_overworld_preset(GameTestHelper h) {
        if (SulfurSurfaceRules.terraBlenderLoaded()) {
            h.succeed();
            return;
        }
        StringBuilder report = new StringBuilder();
        for (ResourceKey<NoiseGeneratorSettings> preset : List.of(
            NoiseGeneratorSettings.OVERWORLD, NoiseGeneratorSettings.LARGE_BIOMES, NoiseGeneratorSettings.AMPLIFIED)) {
            WorldgenProbe probe = new WorldgenProbe(h, preset, 12345L);
            int radius = preset == NoiseGeneratorSettings.LARGE_BIOMES ? 12288 : 4096;
            ChunkPos at = probe.find(ModBiomes.SULFUR_CAVES, radius, 0, 16, 32, -16, 48);
            h.assertTrue(at != null, preset.location() + ": no sulfur caves within " + radius);
            int bands = 0, inBiome = 0;
            for (int dx = 0; dx <= 1 && bands == 0; dx++) {
                ProtoChunk chunk = probe.generate(new ChunkPos(at.x + dx, at.z));
                inBiome += WorldgenProbe.count(chunk, p -> WorldgenProbe.chunkBiome(chunk, p).is(ModBiomes.SULFUR_CAVES), BlockState::isSolid);
                bands += WorldgenProbe.count(chunk, p -> WorldgenProbe.chunkBiome(chunk, p).is(ModBiomes.SULFUR_CAVES),
                    s -> s.is(ModBlocks.SULFUR.get()) || s.is(ModBlocks.CINNABAR.get()));
            }
            report.append(preset.location()).append(" at ").append(at).append(": ").append(bands).append(" band blocks in ")
                .append(inBiome).append(" solid sulfur-caves blocks\n");
            h.assertTrue(bands > 0, preset.location() + ": no sulfur/cinnabar bands in the sulfur caves chunk " + at);
        }
        VBParity.LOG.info("[sulfur_bands_in_every_overworld_preset]\n{}", report);
        h.succeed();
    }

    /** No bands outside sulfur caves (the band rule is biome-gated), checked on a plain overworld chunk far from them. */
    @GameTest(template = BOX, timeoutTicks = 200)
    public static void sulfur_bands_only_in_sulfur_caves(GameTestHelper h) {
        WorldgenProbe probe = new WorldgenProbe(h, NoiseGeneratorSettings.OVERWORLD, 12345L);
        ChunkPos plains = probe.find(Biomes.PLAINS, 4096, 64);
        h.assertTrue(plains != null, "no plains");
        ProtoChunk chunk = probe.generate(plains);
        boolean sulfurNear = false;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = -64; y < 320 && !sulfurNear; y += 4) {
            for (int dx = -16; dx <= 32 && !sulfurNear; dx += 8) {
                for (int dz = -16; dz <= 32; dz += 8) {
                    if (probe.biomeAt(plains.getMinBlockX() + dx, y, plains.getMinBlockZ() + dz).is(ModBiomes.SULFUR_CAVES)) {
                        sulfurNear = true;
                        break;
                    }
                }
            }
        }
        if (!sulfurNear) {
            int bands = WorldgenProbe.count(chunk, q -> true, s -> s.is(ModBlocks.SULFUR.get()) || s.is(ModBlocks.CINNABAR.get()));
            h.assertTrue(bands == 0, "sulfur/cinnabar bands outside sulfur caves: " + bands);
        }
        h.succeed();
    }
}
