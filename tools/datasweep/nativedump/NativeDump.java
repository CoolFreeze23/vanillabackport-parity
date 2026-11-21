package dump;

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
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Dumps block properties and item default components of a 26.x client jar (same JSON shape as the 1.21.1 dump). */
public class NativeDump {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        JsonObject root = new JsonObject();
        root.add("blocks", blocks());
        root.add("items", items());
        Path out = Path.of(args[0]);
        Files.createDirectories(out.getParent());
        try (Writer w = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root, w);
        }
        System.out.println("wrote " + out.toAbsolutePath());
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
        o.addProperty("break", t.getBreakSound().location().toString());
        o.addProperty("step", t.getStepSound().location().toString());
        o.addProperty("place", t.getPlaceSound().location().toString());
        o.addProperty("hit", t.getHitSound().location().toString());
        o.addProperty("fall", t.getFallSound().location().toString());
        return o;
    }

    /** One value if every state agrees, else {state: value}. */
    static JsonElement perState(Block b, Function<BlockState, JsonElement> f) {
        Map<String, JsonElement> m = new LinkedHashMap<>();
        for (BlockState s : b.getStateDefinition().getPossibleStates()) {
            JsonElement v;
            try { v = f.apply(s); } catch (Throwable t) { v = p("n/a"); }
            m.put(s.toString(), v);
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
            all.add(BuiltInRegistries.BLOCK.getKey(b).toString(), o);
        }
        return all;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static JsonObject items() {
        HolderLookup.Provider regs;
        try {
            regs = (HolderLookup.Provider) VanillaRegistries.class.getMethod("createLookup").invoke(null);
        } catch (ReflectiveOperationException e) {
            try {
                regs = (HolderLookup.Provider) VanillaRegistries.class.getMethod("createWorldLookup").invoke(null);
            } catch (ReflectiveOperationException e2) { throw new RuntimeException(e2); }
        }
        RegistryOps<JsonElement> ops = regs.createSerializationContext(JsonOps.INSTANCE);
        JsonObject all = new JsonObject();
        for (DataComponentInitializers.PendingComponents<?> pending : BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(regs)) {
            if (!pending.key().equals(Registries.ITEM)) continue;
            ((DataComponentInitializers.PendingComponents) pending).forEach((holder, comps) -> {
                Item item = (Item) ((net.minecraft.core.Holder.Reference) holder).value();
                JsonObject o = new JsonObject();
                o.addProperty("class", item.getClass().getName());
                DataComponentPatch patch = DataComponentPatch.builder().set((DataComponentMap) comps).build();
                JsonElement enc = DataComponentPatch.CODEC.encodeStart(ops, patch)
                    .resultOrPartial(err -> System.err.println("encode " + holder + ": " + err)).orElse(new JsonObject());
                o.add("components", enc);
                all.add(((net.minecraft.core.Holder.Reference<?>) holder).key().identifier().toString(), o);
            });
        }
        return all;
    }
}
