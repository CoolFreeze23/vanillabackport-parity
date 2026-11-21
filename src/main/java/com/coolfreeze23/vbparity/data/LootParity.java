package com.coolfreeze23.vbparity.data;

import com.coolfreeze23.vbparity.data.mixin.LootItemAccessor;
import com.coolfreeze23.vbparity.data.mixin.LootPoolAccessor;
import com.coolfreeze23.vbparity.data.mixin.LootTableAccessor;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import org.slf4j.Logger;

/**
 * Native loot layout on top of whatever is loaded (VB's files and its LootTableLoadEvent modifiers run first; other
 * mods' global loot modifiers are untouched). Driven by {@code vbparity/loot_parity.json}
 * (tools/datasweep/gen_loot.py):
 * <ul>
 *   <li>{@code random_sequence}: the native per-table random sequence for VB tables that have none;</li>
 *   <li>{@code placements}: the chest entries VB appends at the end of a pool are moved to their native position (with
 *   the native entry), only when VB added them (its config gate);</li>
 *   <li>{@code insert_pools} / {@code replace_pools}: native pools VB lacks or has different.</li>
 * </ul>
 */
@EventBusSubscriber(modid = "vbparity")
public final class LootParity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static JsonObject spec;

    private LootParity() {
    }

    static synchronized JsonObject spec() {
        if (spec == null) {
            try (Reader r = new InputStreamReader(LootParity.class.getResourceAsStream("/vbparity/loot_parity.json"), StandardCharsets.UTF_8)) {
                spec = JsonParser.parseReader(r).getAsJsonObject();
            } catch (Exception e) {
                throw new IllegalStateException("vbparity/loot_parity.json", e);
            }
        }
        return spec;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLoad(LootTableLoadEvent event) {
        String id = event.getName().toString();
        LootTable table = event.getTable();
        DynamicOps<JsonElement> ops = event.getRegistries() != null
            ? RegistryOps.create(JsonOps.INSTANCE, event.getRegistries()) : JsonOps.INSTANCE;
        JsonObject s = spec();
        LootTableAccessor acc = (LootTableAccessor) table;

        JsonElement seq = s.getAsJsonObject("random_sequence").get(id);
        if (seq != null && acc.vbparity$data$getRandomSequence().isEmpty()) {
            acc.vbparity$data$setRandomSequence(Optional.of(ResourceLocation.parse(seq.getAsString())));
        }
        for (JsonElement e : s.getAsJsonArray("replace_pools")) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("table").getAsString().equals(id)) {
                List<LootPool> pools = new ArrayList<>(acc.vbparity$data$getPools());
                int i = o.get("index").getAsInt();
                if (i < pools.size()) {
                    pools.set(i, LootPool.CODEC.parse(ops, o.get("pool")).getOrThrow());
                    setPools(acc, pools);
                }
            }
        }
        for (JsonElement e : s.getAsJsonArray("insert_pools")) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("table").getAsString().equals(id)) {
                List<LootPool> pools = new ArrayList<>(acc.vbparity$data$getPools());
                pools.add(Math.min(o.get("index").getAsInt(), pools.size()), LootPool.CODEC.parse(ops, o.get("pool")).getOrThrow());
                setPools(acc, pools);
            }
        }
        for (JsonElement e : s.getAsJsonArray("placements")) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("table").getAsString().equals(id)) {
                List<LootPool> pools = acc.vbparity$data$getPools();
                int i = o.get("pool").getAsInt();
                if (i < pools.size()) {
                    place(pools.get(i), o, ops);
                }
            }
        }
    }

    private static void setPools(LootTableAccessor acc, List<LootPool> pools) {
        List<LootPool> live = acc.vbparity$data$getPools();
        live.clear();
        live.addAll(pools);
    }

    static String name(LootPoolEntryContainer e) {
        if (e instanceof LootItem item) {
            return BuiltInRegistries.ITEM.getKey(((LootItemAccessor) item).vbparity$data$getItem().value()).toString();
        }
        if (e instanceof EmptyLootItem) {
            return "<minecraft:empty>";
        }
        return "<other>";
    }

    /** Index of the n-th (0-based) entry named {@code name}, or -1. */
    private static int nth(List<LootPoolEntryContainer> list, String name, int n) {
        for (int i = 0; i < list.size(); i++) {
            if (name(list.get(i)).equals(name) && n-- == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int occurrence(List<String> order, int index) {
        int n = 0;
        for (int i = 0; i < index; i++) {
            if (order.get(i).equals(order.get(index))) n++;
        }
        return n;
    }

    private static void place(LootPool pool, JsonObject o, DynamicOps<JsonElement> ops) {
        Set<String> items = new HashSet<>();
        o.getAsJsonArray("items").forEach(x -> items.add(x.getAsString()));
        Map<String, String> presentIf = new HashMap<>();
        o.getAsJsonObject("present_if").entrySet().forEach(x -> presentIf.put(x.getKey(), x.getValue().getAsString()));
        List<String> order = new ArrayList<>();
        o.getAsJsonArray("native_order").forEach(x -> order.add(x.getAsString()));
        List<JsonElement> natives = new ArrayList<>();
        o.getAsJsonArray("native_entries").forEach(natives::add);

        LootPoolAccessor pa = (LootPoolAccessor) pool;
        List<LootPoolEntryContainer> cur = new ArrayList<>(pa.vbparity$data$getEntries());
        Set<String> present = new HashSet<>();
        cur.forEach(e -> present.add(name(e)));
        if (items.stream().noneMatch(present::contains)) {
            return; // VB added none of them (its feature is off): leave the pool alone
        }
        cur.removeIf(e -> items.contains(name(e)));
        int k = 0;
        for (int i = 0; i < order.size(); i++) {
            String n = order.get(i);
            if (!items.contains(n)) continue;
            JsonElement json = natives.get(k++);
            if (!present.contains(presentIf.getOrDefault(n, n))) continue;
            LootPoolEntryContainer entry = LootPoolEntries.CODEC.parse(ops, json).getOrThrow();
            int at = -1;
            if (i > 0) {
                int p = nth(cur, order.get(i - 1), occurrence(order, i - 1));
                if (p >= 0) at = p + 1;
            }
            if (at < 0) {
                for (int j = i + 1; j < order.size() && at < 0; j++) {
                    if (items.contains(order.get(j))) continue;
                    int q = nth(cur, order.get(j), occurrence(order, j));
                    if (q >= 0) at = q;
                    break;
                }
            }
            for (int j = i - 2; j >= 0 && at < 0; j--) {
                int p = nth(cur, order.get(j), occurrence(order, j));
                if (p >= 0) at = p + 1;
            }
            if (at < 0) at = cur.size();
            cur.add(at, entry);
        }
        pa.vbparity$data$setEntries(List.copyOf(cur));
        LOGGER.debug("vbparity: native entry positions in a pool of {}", o.get("table").getAsString());
    }
}
