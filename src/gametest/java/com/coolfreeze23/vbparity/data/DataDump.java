package com.coolfreeze23.vbparity.data;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Writes the effective 1.21.1 + VB + add-on state (block properties, item default components, loot tables after code
 * modifiers, villager and wandering-trader offers) as JSON, in the same shape as the native 26.x dump, for
 * tools/datasweep/props.py. Only runs when {@code vbparity-dump.request} exists in the game directory.
 */
public final class DataDump {
    private DataDump() {}

    public static boolean requested() {
        return Files.exists(Path.of("vbparity-dump.request"));
    }

    public static void write(ServerLevel level) throws Exception {
        MinecraftServer server = level.getServer();
        RegistryOps<JsonElement> ops = level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        JsonObject root = new JsonObject();
        root.add("blocks", blocks());
        root.add("items", items(ops));
        root.add("loot", loot(server, ops));
        root.add("trades", trades(level, ops));
        JsonObject recipes = new JsonObject();
        for (var holder : server.getRecipeManager().getRecipes()) {
            recipes.add(holder.id().toString(), net.minecraft.world.item.crafting.Recipe.CODEC.encodeStart(ops, holder.value())
                .resultOrPartial(err -> {}).orElseGet(JsonObject::new));
        }
        root.add("recipes", recipes);
        Path out = Path.of("vbparity-dump.json");
        try (Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root, w);
        }
    }

    static Object field(Object o, Class<?> c, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    static JsonElement sound(SoundType t) {
        JsonObject o = new JsonObject();
        o.addProperty("volume", t.getVolume());
        o.addProperty("pitch", t.getPitch());
        o.addProperty("break", t.getBreakSound().getLocation().toString());
        o.addProperty("step", t.getStepSound().getLocation().toString());
        o.addProperty("place", t.getPlaceSound().getLocation().toString());
        o.addProperty("hit", t.getHitSound().getLocation().toString());
        o.addProperty("fall", t.getFallSound().getLocation().toString());
        return o;
    }

    static JsonElement perState(Block b, Function<BlockState, JsonElement> f) {
        Map<String, JsonElement> m = new LinkedHashMap<>();
        for (BlockState s : b.getStateDefinition().getPossibleStates()) {
            m.put(s.toString(), f.apply(s));
        }
        if (m.values().stream().distinct().count() == 1) {
            return m.values().iterator().next();
        }
        JsonObject o = new JsonObject();
        m.forEach(o::add);
        return o;
    }

    static JsonElement p(Object v) {
        JsonArray a = new JsonArray();
        if (v instanceof Number n) a.add(n);
        else if (v instanceof Boolean bo) a.add(bo);
        else a.add(String.valueOf(v));
        return a.get(0);
    }

    @SuppressWarnings("unchecked")
    static JsonObject blocks() throws Exception {
        FireBlock fire = (FireBlock) Blocks.FIRE;
        Object2IntMap<Block> ignite = (Object2IntMap<Block>) field(fire, FireBlock.class, "igniteOdds");
        Object2IntMap<Block> burn = (Object2IntMap<Block>) field(fire, FireBlock.class, "burnOdds");
        JsonObject all = new JsonObject();
        EmptyBlockGetter g = EmptyBlockGetter.INSTANCE;
        BlockPos z = BlockPos.ZERO;
        for (Block b : BuiltInRegistries.BLOCK) {
            JsonObject o = new JsonObject();
            o.addProperty("class", b.getClass().getName());
            o.addProperty("destroy_time", b.defaultDestroyTime());
            o.addProperty("explosion_resistance", b.getExplosionResistance());
            o.addProperty("friction", b.getFriction());
            o.addProperty("speed_factor", b.getSpeedFactor());
            o.addProperty("jump_factor", b.getJumpFactor());
            o.addProperty("has_collision", (Boolean) field(b, BlockBehaviour.class, "hasCollision"));
            o.addProperty("ignite_odds", ignite.getInt(b));
            o.addProperty("burn_odds", burn.getInt(b));
            o.add("map_color", perState(b, s -> p(s.getMapColor(g, z).id)));
            o.add("light", perState(b, s -> p(s.getLightEmission())));
            o.add("destroy_speed", perState(b, s -> p(s.getDestroySpeed(g, z))));
            o.add("push_reaction", perState(b, s -> p(s.getPistonPushReaction().name())));
            o.add("requires_correct_tool", perState(b, s -> p(s.requiresCorrectToolForDrops())));
            o.add("sound", perState(b, s -> sound(s.getSoundType())));
            o.add("ignited_by_lava", perState(b, s -> p(s.ignitedByLava())));
            o.add("replaceable", perState(b, s -> p(s.canBeReplaced())));
            o.add("random_ticks", perState(b, s -> p(s.isRandomlyTicking())));
            o.add("instrument", perState(b, s -> p(s.instrument().getSerializedName())));
            o.add("solid", perState(b, s -> p(s.isSolid())));
            o.add("liquid", perState(b, s -> p(s.liquid())));
            o.add("blocks_motion", perState(b, s -> p(s.blocksMotion())));
            o.add("can_occlude", perState(b, s -> p(s.canOcclude())));
            o.add("offset", perState(b, s -> p(s.hasOffsetFunction())));
            o.add("large_collision", perState(b, s -> p(s.hasLargeCollisionShape())));
            o.add("flammability", perState(b, s -> p(s.getFlammability(g, z, net.minecraft.core.Direction.UP))));
            o.add("fire_spread", perState(b, s -> p(s.getFireSpreadSpeed(g, z, net.minecraft.core.Direction.UP))));
            all.add(BuiltInRegistries.BLOCK.getKey(b).toString(), o);
        }
        return all;
    }

    static JsonObject items(RegistryOps<JsonElement> ops) {
        JsonObject all = new JsonObject();
        for (Item item : BuiltInRegistries.ITEM) {
            JsonObject o = new JsonObject();
            o.addProperty("class", item.getClass().getName());
            o.add("components", net.minecraft.core.component.DataComponentMap.CODEC.encodeStart(ops, item.components())
                .resultOrPartial(err -> {}).orElse(new JsonObject()));
            var stack = new net.minecraft.world.item.ItemStack(item);
            o.add("attributes", net.minecraft.world.item.component.ItemAttributeModifiers.CODEC.encodeStart(ops, stack.getAttributeModifiers())
                .resultOrPartial(err -> {}).orElse(new JsonObject()));
            o.addProperty("enchantment_value", stack.getEnchantmentValue());
            o.addProperty("max_stack", stack.getMaxStackSize());
            o.addProperty("max_damage", stack.getMaxDamage());
            all.add(BuiltInRegistries.ITEM.getKey(item).toString(), o);
        }
        return all;
    }

    static JsonObject loot(MinecraftServer server, RegistryOps<JsonElement> ops) {
        JsonObject all = new JsonObject();
        for (ResourceLocation id : server.reloadableRegistries().getKeys(Registries.LOOT_TABLE)) {
            LootTable t = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
            all.add(id.toString(), LootTable.DIRECT_CODEC.encodeStart(ops, t).resultOrPartial(err -> {})
                .orElseGet(() -> {
                    JsonObject e = new JsonObject();
                    e.addProperty("error", "not encodable");
                    return e;
                }));
        }
        return all;
    }

    static JsonArray offers(VillagerTrades.ItemListing[] listings, net.minecraft.world.entity.Entity trader, RegistryOps<JsonElement> ops) {
        JsonArray arr = new JsonArray();
        for (VillagerTrades.ItemListing l : listings) {
            JsonArray samples = new JsonArray();
            for (int seed = 0; seed < 3; seed++) {
                MerchantOffer offer = l.getOffer(trader, RandomSource.create(seed));
                if (offer == null) {
                    samples.add("null");
                    continue;
                }
                JsonElement e = MerchantOffer.CODEC.encodeStart(ops, offer).resultOrPartial(err -> {}).orElse(new JsonObject());
                if (!samples.contains(e)) samples.add(e);
            }
            JsonObject o = new JsonObject();
            o.addProperty("class", l.getClass().getName());
            o.add("samples", samples);
            arr.add(o);
        }
        return arr;
    }

    static JsonObject trades(ServerLevel level, RegistryOps<JsonElement> ops) {
        JsonObject all = new JsonObject();
        Villager v = EntityType.VILLAGER.create(level);
        JsonObject vill = new JsonObject();
        for (var e : VillagerTrades.TRADES.entrySet()) {
            v.setVillagerData(new VillagerData(VillagerType.PLAINS, e.getKey(), 1));
            JsonObject byLevel = new JsonObject();
            e.getValue().int2ObjectEntrySet().forEach(lv -> byLevel.add(String.valueOf(lv.getIntKey()), offers(lv.getValue(), v, ops)));
            vill.add(BuiltInRegistries.VILLAGER_PROFESSION.getKey(e.getKey()).toString(), byLevel);
        }
        all.add("villager", vill);
        WanderingTrader w = EntityType.WANDERING_TRADER.create(level);
        JsonObject wand = new JsonObject();
        VillagerTrades.WANDERING_TRADER_TRADES.int2ObjectEntrySet().forEach(lv -> wand.add(String.valueOf(lv.getIntKey()), offers(lv.getValue(), w, ops)));
        all.add("wandering_trader", wand);
        return all;
    }
}
