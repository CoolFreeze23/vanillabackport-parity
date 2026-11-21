package com.coolfreeze23.vbparity.flora.worldgen;

import com.coolfreeze23.vbparity.VBParity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Graph;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.jetbrains.annotations.Nullable;

/**
 * FLORA-12. Natively a feature's decoration seed is its index in the global per-step feature order that FeatureSorter
 * builds from every possible biome's feature list, and features are placed in that order. VB's world has different
 * lists (1.21.1-only features, VB features at other places), so every index (and seed) shifts. This rebuilds the native
 * order with the native biome lists (26.2; 26.3-pre-1 for chunks that touch a Wilderness Bound biome) and gives each
 * feature its native index. Features that have no native counterpart (1.21.1-only or other mods') keep their own order
 * after the native ones.
 */
public final class NativeFeatureOrder {
    public static final String V262 = "26.2";
    public static final String V263 = "26.3";

    /** Runtime (1.21.1) ids whose native feature has another id. */
    static final Map<String, String> RUNTIME_TO_NATIVE = Map.of(
        "minecraft:trees_birch_and_oak", "minecraft:trees_birch_and_oak_leaf_litter"
    );

    private static volatile Map<String, Map<String, List<List<String>>>> data;

    private NativeFeatureOrder() {}

    public static Map<String, List<List<String>>> nativeLists(String version) {
        Map<String, Map<String, List<List<String>>>> d = data;
        if (d == null) {
            synchronized (NativeFeatureOrder.class) {
                if (data == null) {
                    data = load();
                }
                d = data;
            }
        }
        return d.get(version);
    }

    private static Map<String, Map<String, List<List<String>>>> load() {
        try (InputStream in = NativeFeatureOrder.class.getResourceAsStream("/vbparity/native_biome_features.json")) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, Map<String, List<List<String>>>> out = new HashMap<>();
            for (Map.Entry<String, JsonElement> version : root.entrySet()) {
                Map<String, List<List<String>>> biomes = new HashMap<>();
                for (Map.Entry<String, JsonElement> biome : version.getValue().getAsJsonObject().entrySet()) {
                    List<List<String>> steps = new ArrayList<>();
                    for (JsonElement step : biome.getValue().getAsJsonArray()) {
                        List<String> ids = new ArrayList<>();
                        for (JsonElement id : (JsonArray) step) {
                            ids.add(id.getAsString());
                        }
                        steps.add(List.copyOf(ids));
                    }
                    biomes.put(biome.getKey(), List.copyOf(steps));
                }
                out.put(version.getKey(), Map.copyOf(biomes));
            }
            return Map.copyOf(out);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read vbparity/native_biome_features.json", e);
        }
    }

    private record Node(int featureIndex, int step, String id) {}

    /** FeatureSorter.buildFeaturesPerStep over feature ids (same graph, same DFS, same tie order). */
    public static List<List<String>> sort(List<List<List<String>>> sources) {
        Object2IntMap<String> featureIndex = new Object2IntOpenHashMap<>();
        int[] next = {0};
        Comparator<Node> comparator = Comparator.comparingInt(Node::step).thenComparingInt(Node::featureIndex);
        Map<Node, Set<Node>> edges = new TreeMap<>(comparator);
        int maxStep = 0;
        for (List<List<String>> steps : sources) {
            List<Node> nodes = new ArrayList<>();
            maxStep = Math.max(maxStep, steps.size());
            for (int step = 0; step < steps.size(); step++) {
                for (String id : steps.get(step)) {
                    nodes.add(new Node(featureIndex.computeIfAbsent(id, k -> next[0]++), step, id));
                }
            }
            for (int i = 0; i < nodes.size(); i++) {
                Set<Node> out = edges.computeIfAbsent(nodes.get(i), k -> new TreeSet<>(comparator));
                if (i < nodes.size() - 1) {
                    out.add(nodes.get(i + 1));
                }
            }
        }
        Set<Node> discovered = new TreeSet<>(comparator);
        Set<Node> visiting = new TreeSet<>(comparator);
        List<Node> sorted = new ArrayList<>();
        for (Node node : edges.keySet()) {
            if (!discovered.contains(node) && Graph.depthFirstSearch(edges, discovered, visiting, sorted::add, node)) {
                throw new IllegalStateException("Feature order cycle found");
            }
        }
        Collections.reverse(sorted);
        List<List<String>> perStep = new ArrayList<>();
        for (int step = 0; step < maxStep; step++) {
            int s = step;
            perStep.add(sorted.stream().filter(n -> n.step() == s).map(Node::id).toList());
        }
        return perStep;
    }

    /**
     * The native biome order: the possible-biome order of this world, which matches native once sulfur caves are emitted
     * at their native place (before deep dark; Platform appends them at the end). Only biomes with a native list.
     */
    public static List<String> nativeBiomeOrder(Iterable<Holder<Biome>> possibleBiomes, Map<String, List<List<String>>> lists) {
        List<String> order = new ArrayList<>();
        for (Holder<Biome> biome : possibleBiomes) {
            biome.unwrapKey().map(k -> k.location().toString()).filter(lists::containsKey).ifPresent(order::add);
        }
        int sulfur = order.indexOf("minecraft:sulfur_caves");
        int deepDark = order.indexOf("minecraft:deep_dark");
        if (sulfur > deepDark && deepDark >= 0) {
            order.remove(sulfur);
            order.add(deepDark, "minecraft:sulfur_caves");
        }
        return order;
    }

    public static List<List<String>> nativeOrder(Iterable<Holder<Biome>> possibleBiomes, String version) {
        Map<String, List<List<String>>> lists = nativeLists(version);
        List<List<List<String>>> sources = new ArrayList<>();
        for (String biome : nativeBiomeOrder(possibleBiomes, lists)) {
            sources.add(lists.get(biome));
        }
        return sort(sources);
    }

    /**
     * The runtime step data re-indexed to native indices: a native feature sits at its native index (= its native seed),
     * features without a native counterpart follow at {@code nativeCount + runtimeIndex}. Unused slots are null; the
     * decoration loop only reads indices of features that are present.
     */
    public static List<FeatureSorter.StepFeatureData> remap(
        List<FeatureSorter.StepFeatureData> runtime, List<List<String>> nativeOrder, Registry<PlacedFeature> registry
    ) {
        List<FeatureSorter.StepFeatureData> out = new ArrayList<>(runtime.size());
        for (int step = 0; step < runtime.size(); step++) {
            List<String> natives = step < nativeOrder.size() ? nativeOrder.get(step) : List.of();
            Object2IntMap<String> nativeIndex = new Object2IntOpenHashMap<>();
            nativeIndex.defaultReturnValue(-1);
            for (int i = 0; i < natives.size(); i++) {
                nativeIndex.putIfAbsent(natives.get(i), i);
            }
            List<PlacedFeature> features = runtime.get(step).features();
            int n = natives.size();
            PlacedFeature[] slots = new PlacedFeature[n + features.size()];
            Reference2IntOpenHashMap<PlacedFeature> index = new Reference2IntOpenHashMap<>();
            index.defaultReturnValue(-1);
            for (int i = 0; i < features.size(); i++) {
                PlacedFeature feature = features.get(i);
                int at = nativeIndexOf(registry.getKey(feature), nativeIndex);
                if (at < 0 || slots[at] != null) {
                    at = n + i;
                }
                slots[at] = feature;
                index.put(feature, at);
            }
            out.add(new FeatureSorter.StepFeatureData(Collections.unmodifiableList(Arrays.asList(slots)), index::getInt));
        }
        return List.copyOf(out);
    }

    static int nativeIndexOf(@Nullable ResourceLocation key, Object2IntMap<String> nativeIndex) {
        return key == null ? -1 : nativeIndex.getInt(nativeIdOf(key.toString()));
    }

    /** The native id of a runtime feature id. */
    public static String nativeIdOf(String runtimeId) {
        return RUNTIME_TO_NATIVE.getOrDefault(runtimeId, runtimeId);
    }

    /** Remapped step data for both versions, computed once per feature-order instance of a generator. */
    public record Remapped(List<FeatureSorter.StepFeatureData> source, List<FeatureSorter.StepFeatureData> v262,
                           List<FeatureSorter.StepFeatureData> v263) {
        public static Remapped of(List<FeatureSorter.StepFeatureData> source, Iterable<Holder<Biome>> possibleBiomes,
                                  Registry<PlacedFeature> registry) {
            try {
                return new Remapped(source, remap(source, nativeOrder(possibleBiomes, V262), registry),
                    remap(source, nativeOrder(possibleBiomes, V263), registry));
            } catch (RuntimeException e) {
                VBParity.LOG.warn("Cannot rebuild the native feature order ({}); keeping the runtime order", e.toString());
                return new Remapped(source, source, source);
            }
        }
    }

    /** For tests and logs: the native biome order used for a version. */
    public static LinkedHashMap<String, List<List<String>>> nativeSources(Iterable<Holder<Biome>> possibleBiomes, String version) {
        Map<String, List<List<String>>> lists = nativeLists(version);
        LinkedHashMap<String, List<List<String>>> out = new LinkedHashMap<>();
        for (String biome : nativeBiomeOrder(possibleBiomes, lists)) {
            out.put(biome, lists.get(biome));
        }
        return out;
    }
}
