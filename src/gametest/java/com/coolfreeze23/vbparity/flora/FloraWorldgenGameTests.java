package com.coolfreeze23.vbparity.flora;

import com.coolfreeze23.vbparity.VBParity;
import com.coolfreeze23.vbparity.flora.worldgen.NativeFeatureOrder;
import com.coolfreeze23.vbparity.flora.worldgen.NativeStepsAccess;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** FLORA-12 and worldgen placement end to end, on a real overworld noise generator built from the server registries. */
@GameTestHolder(VBParity.MOD_ID)
@PrefixGameTestTemplate(false)
public class FloraWorldgenGameTests {
    private static final String T = "flora_empty";

    /** Features the 26.2 overworld lists that a 1.21.1 + VB world cannot have (vanilla-only changes outside VB). */
    private static final Set<String> NATIVE_ONLY_OK = Set.of("minecraft:patch_grass_meadow");

    private static String id(Registry<PlacedFeature> reg, PlacedFeature f) {
        ResourceLocation k = reg.getKey(f);
        return k == null ? "inline" : k.toString();
    }

    private static List<List<List<String>>> runtimeSources(WorldgenProbe probe) {
        List<List<List<String>>> out = new ArrayList<>();
        for (Holder<Biome> b : probe.biomeSource.possibleBiomes()) {
            List<List<String>> steps = new ArrayList<>();
            for (HolderSet<PlacedFeature> step : b.value().getGenerationSettings().features()) {
                steps.add(step.stream().map(hf -> hf.unwrapKey().map(k -> k.location().toString()).orElse("inline:" + System.identityHashCode(hf.value()))).toList());
            }
            out.add(steps);
        }
        return out;
    }

    /** The id-based port of FeatureSorter gives exactly the order Minecraft's own sorter gives (same lists, same biomes). */
    @GameTest(template = T)
    public static void flora_native_sorter_port_matches_vanilla(GameTestHelper h) {
        WorldgenProbe probe = new WorldgenProbe(h, NoiseGeneratorSettings.OVERWORLD, 0L);
        Registry<PlacedFeature> reg = h.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        List<FeatureSorter.StepFeatureData> vanilla = FeatureSorter.buildFeaturesPerStep(
            List.copyOf(probe.biomeSource.possibleBiomes()), b -> b.value().getGenerationSettings().features(), true);
        List<List<String>> port = NativeFeatureOrder.sort(runtimeSources(probe));
        h.assertTrue(port.size() == vanilla.size(), "step count " + port.size() + " vs " + vanilla.size());
        for (int s = 0; s < vanilla.size(); s++) {
            List<String> v = vanilla.get(s).features().stream().map(f -> {
                ResourceLocation k = reg.getKey(f);
                return k == null ? null : k.toString();
            }).toList();
            List<String> p = port.get(s).stream().map(x -> x.startsWith("inline:") ? null : x).toList();
            if (!v.equals(p)) {
                throw new AssertionError("step " + s + " differs\nvanilla " + v + "\nport    " + p);
            }
        }
        h.succeed();
    }

    /**
     * Every overworld feature with a native counterpart is placed with its native global index (= its native decoration
     * seed), and each biome's features are placed in the native list order of that biome. 26.2 for every biome,
     * 26.3-pre-1 for the dappled forest. Also the per-biome feature sets against native, step by step.
     */
    @GameTest(template = T)
    public static void flora_native_feature_indices(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        WorldgenProbe probe = new WorldgenProbe(h, NoiseGeneratorSettings.OVERWORLD, 0L);
        Registry<PlacedFeature> reg = level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        StringBuilder problems = new StringBuilder();
        StringBuilder extras = new StringBuilder();
        for (boolean wb : new boolean[]{false, true}) {
            String version = wb ? NativeFeatureOrder.V263 : NativeFeatureOrder.V262;
            List<FeatureSorter.StepFeatureData> nat = ((NativeStepsAccess) probe.generator).vbparity$nativeSteps(level, wb);
            List<List<String>> nativeOrder = NativeFeatureOrder.nativeOrder(probe.biomeSource.possibleBiomes(), version);
            Map<String, List<List<String>>> lists = NativeFeatureOrder.nativeLists(version);
            for (Holder<Biome> b : probe.biomeSource.possibleBiomes()) {
                String bid = b.unwrapKey().orElseThrow().location().toString();
                List<List<String>> natLists = lists.get(bid);
                if (natLists == null || (!wb && bid.equals("minecraft:dappled_forest")) || (wb && !bid.equals("minecraft:dappled_forest"))) {
                    continue;
                }
                List<HolderSet<PlacedFeature>> steps = b.value().getGenerationSettings().features();
                for (int s = 0; s < Math.max(steps.size(), natLists.size()); s++) {
                    List<String> natList = s < natLists.size() ? natLists.get(s) : List.of();
                    // runtime features of this biome+step in placement (index) order
                    TreeMap<Integer, String> placed = new TreeMap<>();
                    if (s < steps.size()) {
                        for (Holder<PlacedFeature> f : steps.get(s)) {
                            int idx = nat.get(s).indexMapping().applyAsInt(f.value());
                            String fid = id(reg, f.value());
                            String nid = NativeFeatureOrder.nativeIdOf(fid);
                            int expected = nativeOrder.get(s).indexOf(nid);
                            if (expected >= 0 && idx != expected) {
                                problems.append(version).append(' ').append(bid).append(" step ").append(s).append(' ').append(fid)
                                    .append(" index ").append(idx).append(" expected ").append(expected).append('\n');
                            }
                            if (nat.get(s).features().get(idx) != f.value()) {
                                problems.append(version).append(' ').append(bid).append(' ').append(fid).append(" slot mismatch\n");
                            }
                            placed.put(idx, nid);
                        }
                    }
                    List<String> order = new ArrayList<>(placed.values());
                    List<String> nativePresent = natList.stream().filter(order::contains).toList();
                    List<String> runtimeNative = order.stream().filter(natList::contains).toList();
                    if (!nativePresent.equals(runtimeNative)) {
                        problems.append(version).append(' ').append(bid).append(" step ").append(s).append(" order ").append(runtimeNative)
                            .append(" native ").append(nativePresent).append('\n');
                    }
                    Set<String> missing = new HashSet<>(natList);
                    missing.removeAll(order);
                    missing.removeAll(NATIVE_ONLY_OK);
                    if (!missing.isEmpty()) {
                        problems.append(version).append(' ').append(bid).append(" step ").append(s).append(" missing native ").append(missing).append('\n');
                    }
                    Set<String> extra = new HashSet<>(order);
                    extra.removeAll(natList);
                    if (!extra.isEmpty()) {
                        extras.append(bid).append(" step ").append(s).append(' ').append(extra).append('\n');
                    }
                }
            }
        }
        VBParity.LOG.info("[flora_native_feature_indices] runtime features without a native counterpart:\n{}", extras);
        if (!problems.isEmpty()) {
            throw new AssertionError("native feature order problems:\n" + problems);
        }
        h.succeed();
    }

    /**
     * Placement end to end: a 3x3-chunk area of the flat test world far from the tests, painted with one biome, is
     * decorated by the overworld noise generator (native feature order and seeds), and the biome's VB features appear.
     */
    @GameTest(template = T, timeoutTicks = 400)
    public static void flora_decoration_end_to_end(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        WorldgenProbe probe = new WorldgenProbe(h, NoiseGeneratorSettings.OVERWORLD, level.getSeed());
        decorate(h, probe, new ChunkPos(400, 400), "birch_forest", "wildflowers", "birch_log");
        // patch_bush is a 1-in-4 rarity filter: some of four plains chunks get a bush
        Map<String, Integer> plains = new TreeMap<>();
        for (int i = 0; i < 4; i++) {
            decorate(h, probe, new ChunkPos(420 + 4 * i, 420), "plains").forEach((k, v) -> plains.merge(k, v, Integer::sum));
        }
        h.assertTrue(plains.getOrDefault("bush", 0) > 0 && plains.getOrDefault("short_grass", 0) > 0, "plains: no bush/short grass in 4 chunks: " + plains);
        decorate(h, probe, new ChunkPos(440, 400), "forest", "leaf_litter", "oak_log");
        decorate(h, probe, new ChunkPos(460, 400), "dappled_forest", "poplar_log", "leaf_litter");
        h.succeed();
    }

    private static Map<String, Integer> decorate(GameTestHelper h, WorldgenProbe probe, ChunkPos center, String biome, String... expect) {
        ServerLevel level = h.getLevel();
        Holder<Biome> holder = probe.biomes.getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace(biome)));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                LevelChunk c = level.getChunk(center.x + dx, center.z + dz);
                WorldgenProbe.paintBiome(c, holder, probe.randomState);
            }
        }
        LevelChunk chunk = level.getChunk(center.x, center.z);
        probe.generator.applyBiomeDecoration(level, chunk, level.structureManager());
        Map<String, Integer> counts = new TreeMap<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = level.getMinBuildHeight(); y < level.getMinBuildHeight() + 60; y++) {
            for (int x = center.getMinBlockX() - 8; x < center.getMaxBlockX() + 8; x++) {
                for (int z = center.getMinBlockZ() - 8; z < center.getMaxBlockZ() + 8; z++) {
                    BlockState s = level.getBlockState(p.set(x, y, z));
                    if (!s.isAir()) {
                        counts.merge(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath(), 1, Integer::sum);
                    }
                }
            }
        }
        VBParity.LOG.info("[flora_decoration_end_to_end] {} at {}: {}", biome, center, counts);
        for (String e : expect) {
            h.assertTrue(counts.getOrDefault(e, 0) > 0, biome + ": no " + e + " placed; blocks " + counts);
        }
        return counts;
    }
}
