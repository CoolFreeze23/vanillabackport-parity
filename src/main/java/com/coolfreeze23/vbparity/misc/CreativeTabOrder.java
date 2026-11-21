package com.coolfreeze23.vbparity.misc;

import com.coolfreeze23.vbparity.VBParity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Puts every backported item in a vanilla creative tab at its native place. The item order of each native 26.2 tab
 * (26.3-pre-1 for the Wilderness Bound items) is in {@code vbparity/misc/creative_order.json}, generated from the
 * decompiled {@code CreativeModeTabs}. Items that are new since 1.21.1 are taken out and put back right after the
 * nearest item that comes before them in the native order and is in the tab; 1.21.1 items stay where they are. Only
 * items that are already in the tab move (VB's config still decides what is shown), and the parent and search lists
 * are ordered separately.
 */
public final class CreativeTabOrder {
    private CreativeTabOrder() {}

    private record TabOrder(List<ResourceLocation> order, Set<ResourceLocation> moved) {}

    private static Map<String, TabOrder> orders;

    private static Map<String, TabOrder> orders() {
        if (orders == null) {
            Map<String, TabOrder> loaded = new HashMap<>();
            try (InputStream in = CreativeTabOrder.class.getResourceAsStream("/vbparity/misc/creative_order.json")) {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                    JsonObject tab = entry.getValue().getAsJsonObject();
                    loaded.put(entry.getKey(), new TabOrder(ids(tab.getAsJsonArray("order")), new HashSet<>(ids(tab.getAsJsonArray("new")))));
                }
            } catch (Exception e) {
                VBParity.LOG.warn("Could not read the native creative tab order", e);
            }
            orders = loaded;
        }
        return orders;
    }

    private static List<ResourceLocation> ids(JsonArray array) {
        List<ResourceLocation> ids = new ArrayList<>();
        array.forEach(e -> ids.add(ResourceLocation.withDefaultNamespace(e.getAsString())));
        return ids;
    }

    public static void apply(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation tabId = event.getTabKey().location();
        if (!tabId.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
            return;
        }
        TabOrder tab = orders().get(tabId.getPath());
        if (tab == null) {
            return;
        }
        List<Item> order = new ArrayList<>();
        Set<Item> moved = new HashSet<>();
        for (ResourceLocation id : tab.order()) {
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
            if (item == Items.AIR) {
                continue;
            }
            order.add(item);
            if (tab.moved().contains(id)) {
                moved.add(item);
            }
        }
        reorder(event, new ArrayList<>(event.getParentEntries()), order, moved, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        reorder(event, new ArrayList<>(event.getSearchEntries()), order, moved, CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY);
    }

    private static void reorder(BuildCreativeModeTabContentsEvent event, List<ItemStack> entries, List<Item> order, Set<Item> moved,
                                CreativeModeTab.TabVisibility visibility) {
        Map<Item, List<ItemStack>> taken = new LinkedHashMap<>();
        for (ItemStack stack : entries) {
            if (moved.contains(stack.getItem())) {
                taken.computeIfAbsent(stack.getItem(), item -> new ArrayList<>()).add(stack);
            }
        }
        if (taken.isEmpty()) {
            return;
        }
        taken.values().forEach(stacks -> stacks.forEach(stack -> event.remove(stack, visibility)));
        List<ItemStack> remaining = new ArrayList<>(visibility == CreativeModeTab.TabVisibility.PARENT_TAB_ONLY
                ? event.getParentEntries() : event.getSearchEntries());

        ItemStack previous = null;
        List<ItemStack> beforeFirstAnchor = new ArrayList<>();
        for (Item item : order) {
            List<ItemStack> stacks = taken.remove(item);
            if (stacks != null) {
                for (ItemStack stack : stacks) {
                    if (previous == null) {
                        beforeFirstAnchor.add(stack);
                    } else {
                        event.insertAfter(previous, stack, visibility);
                        previous = stack;
                    }
                }
            } else if (!moved.contains(item)) {
                ItemStack first = null;
                ItemStack last = null;
                for (ItemStack stack : remaining) {
                    if (stack.is(item)) {
                        if (first == null) {
                            first = stack;
                        }
                        last = stack;
                    }
                }
                if (last != null) {
                    if (previous == null) {
                        for (ItemStack stack : beforeFirstAnchor) {
                            event.insertBefore(first, stack, visibility);
                        }
                        beforeFirstAnchor.clear();
                    }
                    previous = last;
                }
            }
        }
        // Nothing to anchor to (an empty tab apart from these items): keep their native order at the end.
        beforeFirstAnchor.forEach(stack -> event.accept(stack, visibility));
        taken.values().forEach(stacks -> stacks.forEach(stack -> event.accept(stack, visibility)));
    }
}
