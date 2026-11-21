package com.coolfreeze23.vbparity.variants;

import com.blackgear.vanillabackport.common.api.modules.mob_variant.ClientAsset;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.chicken.ChickenVariant;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.cow.CowVariant;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.cow.CowVariants;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.pig.PigVariant;
import com.blackgear.vanillabackport.core.util.AdditionalCodecs;
import com.coolfreeze23.vbparity.variants.mixin.access.BuiltInCoreRegistryAccessor;
import com.coolfreeze23.vbparity.variants.sync.VariantRegistrySync;
import com.coolfreeze23.vbparity.variants.sync.VariantSyncPayload;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.CatVariant;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public final class VariantsPass2GameTests {
    private static final String TEMPLATE = "variants_platform";

    private VariantsPass2GameTests() {
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private static int countAllBlack(ServerLevel level, int cats) {
        int allBlack = 0;
        for (int i = 0; i < cats; i++) {
            Cat cat = EntityType.CAT.create(level);
            cat.moveTo(0.5, 0.0, 0.5, 0.0F, 0.0F);
            cat.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.ZERO), MobSpawnType.SPAWN_EGG, null);
            if (cat.getVariant().is(CatVariant.ALL_BLACK)) {
                allBlack++;
            }
            cat.discard();
        }
        return allBlack;
    }

    // Native moon phase is a timeline attribute that only the overworld runs: outside it the moon is always full, so a
    // spawned cat can be all_black. 1.21.1 uses the day count everywhere (a new moon in the Nether too).
    @GameTest(template = TEMPLATE)
    public static void variants_cat_moon_outside_overworld(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        long dayTime = overworld.getDayTime();
        int netherBlack;
        int overworldBlack;
        try {
            overworld.setDayTime(4L * 24000L + 18000L);
            overworld.updateSkyBrightness(); // a new moon night
            check(helper, overworld.getMoonBrightness() == 0.0F, "test setup: not a new moon");
            netherBlack = countAllBlack(nether, 300);
            overworldBlack = countAllBlack(overworld, 300);
        } finally {
            overworld.setDayTime(dayTime);
            overworld.updateSkyBrightness();
        }
        check(helper, netherBlack > 0, "no all_black cat among 300 spawned in the Nether (native: full moon there)");
        check(helper, overworldBlack == 0, overworldBlack + " all_black cats at an overworld new moon (native: none)");
        helper.succeed();
    }

    // AdditionalCodecs.floatRange must accept its lower bound, as native ExtraCodecs.floatRange does.
    @GameTest(template = TEMPLATE)
    public static void variants_float_range_inclusive(GameTestHelper helper) {
        Codec<Float> range = AdditionalCodecs.floatRange(0.0F, 1.0F);
        check(helper, range.parse(JsonOps.INSTANCE, new JsonPrimitive(0.0F)).result().isPresent(), "0.0 rejected by floatRange(0, 1)");
        check(helper, range.encodeStart(JsonOps.INSTANCE, 0.0F).result().isPresent(), "0.0 not encodable by floatRange(0, 1)");
        check(helper, range.parse(JsonOps.INSTANCE, new JsonPrimitive(1.0F)).result().isPresent(), "1.0 rejected by floatRange(0, 1)");
        check(helper, range.parse(JsonOps.INSTANCE, new JsonPrimitive(-0.01F)).result().isEmpty(), "-0.01 accepted by floatRange(0, 1)");
        check(helper, range.parse(JsonOps.INSTANCE, new JsonPrimitive(1.01F)).result().isEmpty(), "1.01 accepted by floatRange(0, 1)");
        helper.succeed();
    }

    private static <T> T parse(GameTestHelper helper, Codec<T> codec, String json) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        return codec.parse(ops, JsonParser.parseString(json)).getOrThrow();
    }

    private static JsonObject encode(GameTestHelper helper, Codec<Object> codec, Object value) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        return codec.encodeStart(ops, value).getOrThrow().getAsJsonObject();
    }

    @SuppressWarnings("unchecked")
    private static Codec<Object> raw(Codec<?> codec) {
        return (Codec<Object>) codec;
    }

    // VARIANTS-10: cow, pig and chicken variants read and write the native baby_asset_id.
    @GameTest(template = TEMPLATE)
    public static void variants_baby_asset_id(GameTestHelper helper) {
        CowVariant cow = parse(helper, CowVariant.CODEC,
            "{\"asset_id\":\"minecraft:entity/cow/cow_cold\",\"baby_asset_id\":\"minecraft:entity/cow/cow_cold_baby\",\"model\":\"cold\",\"spawn_conditions\":[]}");
        ClientAsset cowBaby = BabyAssets.get(cow.modelAndTexture());
        check(helper, cowBaby != null && cowBaby.path().equals(ResourceLocation.withDefaultNamespace("textures/entity/cow/cow_cold_baby.png")),
            "cow baby_asset_id not read: " + cowBaby);
        JsonObject written = encode(helper, raw(CowVariant.CODEC), cow);
        check(helper, "minecraft:entity/cow/cow_cold_baby".equals(written.has(BabyAssets.FIELD) ? written.get(BabyAssets.FIELD).getAsString() : null),
            "cow baby_asset_id not written: " + written);
        PigVariant pig = parse(helper, PigVariant.CODEC,
            "{\"asset_id\":\"minecraft:entity/pig/pig_warm\",\"baby_asset_id\":\"minecraft:entity/pig/pig_warm_baby\",\"spawn_conditions\":[]}");
        check(helper, BabyAssets.get(pig.modelAndTexture()) != null, "pig baby_asset_id not read");
        ChickenVariant chicken = parse(helper, ChickenVariant.CODEC,
            "{\"asset_id\":\"minecraft:entity/chicken/chicken_warm\",\"spawn_conditions\":[]}");
        check(helper, BabyAssets.get(chicken.modelAndTexture()) == null, "a variant without baby_asset_id must have none");
        check(helper, !encode(helper, raw(ChickenVariant.CODEC), chicken).has(BabyAssets.FIELD), "absent baby_asset_id was written");
        helper.succeed();
    }

    // VARIANTS-22: datapack variants are sent to clients with their client data and without spawn conditions,
    // and decode back to the same assets.
    @GameTest(template = TEMPLATE)
    public static void variants_registry_sync(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("vbparity", "sync_test");
        Map<ResourceLocation, Object> dataDriven = ((BuiltInCoreRegistryAccessor) (Object) CowVariants.REGISTRIES).vbp$getDataDriven();
        Map<ResourceLocation, Object> saved = new LinkedHashMap<>(dataDriven);
        try {
            CowVariant variant = parse(helper, CowVariant.CODEC,
                "{\"asset_id\":\"minecraft:entity/cow/cow_warm\",\"baby_asset_id\":\"vbparity:entity/sync_test_baby\",\"model\":\"warm\","
                    + "\"spawn_conditions\":[{\"priority\":1,\"condition\":{\"type\":\"minecraft:biome\",\"biomes\":\"#minecraft:is_savanna\"}}]}");
            check(helper, !variant.spawnConditions().selectors().isEmpty(), "test setup: spawn conditions not parsed");
            CowVariants.REGISTRIES.registerDataDriven(id, variant);

            VariantSyncPayload payload = VariantRegistrySync.build(helper.getLevel().registryAccess());
            List<VariantSyncPayload.Entry> cows = payload.registries().get("cow_variant");
            VariantSyncPayload.Entry entry = cows == null ? null : cows.stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
            check(helper, entry != null, "datapack cow variant missing from the sync payload");
            JsonObject json = JsonParser.parseString(entry.json()).getAsJsonObject();
            check(helper, json.getAsJsonArray("spawn_conditions").isEmpty(), "spawn conditions were sent: " + json);
            check(helper, json.has(BabyAssets.FIELD), "baby_asset_id was not sent: " + json);
            check(helper, cows.stream().noneMatch(e -> e.id().getPath().equals("temperate")), "code-driven variants must not be sent");

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            VariantSyncPayload.STREAM_CODEC.encode(buf, payload);
            VariantSyncPayload received = VariantSyncPayload.STREAM_CODEC.decode(buf);
            check(helper, received.equals(payload), "payload changed over the wire");

            @SuppressWarnings("unchecked")
            VariantRegistrySync.Synced<CowVariant> synced = (VariantRegistrySync.Synced<CowVariant>) VariantRegistrySync.registries().stream()
                .filter(s -> s.name().equals("cow_variant")).findFirst().orElseThrow();
            Map<ResourceLocation, CowVariant> decoded = VariantRegistrySync.decode(synced, received.registries().get("cow_variant"),
                helper.getLevel().registryAccess());
            CowVariant copy = decoded.get(id);
            check(helper, copy != null, "synced cow variant did not decode");
            check(helper, copy.modelAndTexture().model() == CowVariant.ModelType.WARM
                && copy.modelAndTexture().asset().path().equals(ResourceLocation.withDefaultNamespace("textures/entity/cow/cow_warm.png")),
                "synced cow variant lost its model or texture");
            ClientAsset baby = BabyAssets.get(copy.modelAndTexture());
            check(helper, baby != null && baby.id().equals(ResourceLocation.fromNamespaceAndPath("vbparity", "entity/sync_test_baby")),
                "synced cow variant lost its baby texture");
            check(helper, copy.spawnConditions().selectors().isEmpty(), "synced variant kept spawn conditions");
        } finally {
            CowVariants.REGISTRIES.clearDataDrivenEntries();
            saved.forEach((key, value) -> CowVariants.REGISTRIES.registerDataDriven(key, (CowVariant) value));
        }
        check(helper, !CowVariants.REGISTRIES.contains(id), "test entry not removed");
        check(helper, BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.withDefaultNamespace("cow")), "sanity");
        helper.succeed();
    }

    // VARIANTS-21 (persistence): a cat spawned inside a witch hut is not made persistent by finalizeSpawn (native);
    // CatSpawner's hut spawn still makes its cat persistent.
    @GameTest(template = TEMPLATE)
    public static void variants_cat_hut_persistence(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Structure hut = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getOrThrow(BuiltinStructures.SWAMP_HUT);
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        ChunkPos chunkPos = new ChunkPos(origin);
        ServerChunkCache source = level.getChunkSource();
        StructureStart start = hut.generate(level.registryAccess(), source.getGenerator(), source.getGenerator().getBiomeSource(), source.randomState(),
            level.getStructureManager(), level.getSeed(), chunkPos, 0, level, biome -> true);
        check(helper, start.isValid(), "test setup: no swamp hut start");
        LevelChunk chunk = level.getChunk(chunkPos.x, chunkPos.z);
        Map<Structure, StructureStart> starts = new HashMap<>(chunk.getAllStarts());
        Map<Structure, LongSet> references = new HashMap<>();
        chunk.getAllReferences().forEach((structure, set) -> references.put(structure, new LongOpenHashSet(set)));
        try {
            chunk.setStartForStructure(hut, start);
            chunk.addReferenceForStructure(hut, chunkPos.toLong());
            BlockPos inside = start.getBoundingBox().getCenter();
            Cat egg = EntityType.CAT.create(level);
            egg.moveTo(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5, 0.0F, 0.0F);
            egg.finalizeSpawn(level, level.getCurrentDifficultyAt(inside), MobSpawnType.SPAWN_EGG, null);
            check(helper, egg.getVariant().is(CatVariant.ALL_BLACK), "test setup: the hut was not found");
            check(helper, !egg.isPersistenceRequired(), "a spawn-egg cat in a witch hut was made persistent");
            Cat spawned = EntityType.CAT.create(level);
            spawned.moveTo(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5, 0.0F, 0.0F);
            CatHutSpawn.run(() -> {
                spawned.finalizeSpawn(level, level.getCurrentDifficultyAt(inside), MobSpawnType.NATURAL, null);
                return 1;
            });
            check(helper, spawned.isPersistenceRequired(), "a CatSpawner hut cat must stay persistent");
            egg.discard();
            spawned.discard();
        } finally {
            chunk.setAllStarts(starts);
            chunk.setAllReferences(references);
        }
        helper.succeed();
    }
}
