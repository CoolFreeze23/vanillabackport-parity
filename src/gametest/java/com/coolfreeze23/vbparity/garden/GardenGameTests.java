package com.coolfreeze23.vbparity.garden;

import com.blackgear.vanillabackport.common.level.block_entities.CreakingHeartBlockEntity;
import com.blackgear.vanillabackport.common.level.blocks.CreakingHeartBlock;
import com.blackgear.vanillabackport.common.level.blocks.CreakingHeartState;
import com.blackgear.vanillabackport.common.level.blocks.HangingMossBlock;
import com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking;
import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.blackgear.vanillabackport.common.registries.worldgen.ModBiomes;
import com.coolfreeze23.vbparity.garden.mixin.CreakingHeartBlockEntityInvoker;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public class GardenGameTests {
    private static final String SMALL = "garden_small";
    private static final String LANE = "garden_lane";

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new GameTestAssertException(message);
        }
    }

    private static Creaking stillCreaking(GameTestHelper helper, Vec3 pos) {
        Creaking creaking = helper.spawn(ModEntityTypes.CREAKING.get(), pos);
        creaking.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
        return creaking;
    }

    private static boolean sensed(Creaking creaking, Player player) {
        return creaking.getBrain().getMemory(MemoryModuleType.NEAREST_PLAYERS).orElse(List.of()).contains(player);
    }

    // ---- GARDEN-01 -------------------------------------------------------------------------------------------

    /** One spreadResin call places exactly one resin face (native STOP); VB placed one per reachable log. */
    @GameTest(template = SMALL)
    public static void garden_resin_spread_places_one_face(GameTestHelper helper) {
        BlockPos heartPos = new BlockPos(3, 2, 3);
        for (BlockPos pos : BlockPos.betweenClosed(heartPos.offset(-1, -1, -1), heartPos.offset(1, 1, 1))) {
            helper.setBlock(pos, ModBlocks.PALE_OAK_LOG.get());
        }
        helper.setBlock(heartPos, ModBlocks.CREAKING_HEART.get().defaultBlockState().setValue(CreakingHeartBlock.STATE, CreakingHeartState.AWAKE));
        CreakingHeartBlockEntity heart = helper.getBlockEntity(heartPos);
        check(((CreakingHeartBlockEntityInvoker) heart).vbparity$garden$spreadResin(helper.getLevel()).isPresent(), "no resin placed");

        int faces = 0;
        for (BlockPos pos : BlockPos.betweenClosed(heartPos.offset(-2, -2, -2), heartPos.offset(2, 2, 2))) {
            BlockState state = helper.getBlockState(pos);
            if (state.is(ModBlocks.RESIN_CLUMP.get())) {
                for (Direction dir : Direction.values()) {
                    if (MultifaceBlock.hasFace(state, dir)) {
                        faces++;
                    }
                }
            }
        }
        check(faces == 1, "expected 1 resin face per spread, got " + faces);
        helper.succeed();
    }

    // ---- GARDEN-02 -------------------------------------------------------------------------------------------

    /** A survival player 28 blocks away is in the creaking's player memories (native range = follow range 32). */
    @GameTest(template = LANE, batch = "garden_sensor_range", timeoutTicks = 80)
    public static void garden_creaking_senses_player_at_28(GameTestHelper helper) {
        Creaking creaking = stillCreaking(helper, new Vec3(2.5, 1.0, 2.5));
        ServerPlayer player = GardenTestPlayers.survival(helper, new Vec3(30.5, 1.0, 2.5), 80);
        GardenTestPlayers.lookAwayFrom(player, creaking.getEyePosition());
        helper.succeedWhen(() -> {
            check(sensed(creaking, player), "player 28 blocks away is not in NEAREST_PLAYERS");
            check(creaking.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER).orElse(null) == player,
                "player 28 blocks away is not the nearest visible attackable player");
            GardenTestPlayers.remove(player);
        });
    }

    /** A player 28 blocks away who looks at an active creaking freezes it (VB: it deactivates and keeps moving). */
    @GameTest(template = LANE, batch = "garden_sensor_freeze", timeoutTicks = 100)
    public static void garden_creaking_freezes_when_watched_at_28(GameTestHelper helper) {
        Creaking creaking = stillCreaking(helper, new Vec3(2.5, 1.0, 2.5));
        ServerPlayer player = GardenTestPlayers.survival(helper, new Vec3(30.5, 1.0, 2.5), 100);
        GardenTestPlayers.lookAt(player, creaking.getEyePosition());
        helper.startSequence()
            .thenWaitUntil(() -> check(sensed(creaking, player), "player 28 blocks away is not sensed"))
            .thenExecute(() -> creaking.setIsActive(true))
            .thenWaitUntil(() -> check(!creaking.canMove() && creaking.isActive(), "watched creaking did not freeze"))
            .thenExecute(() -> GardenTestPlayers.remove(player))
            .thenSucceed();
    }

    // ---- GARDEN-03 -------------------------------------------------------------------------------------------

    /**
     * Freezing switches FIGHT to IDLE. Native keeps ATTACK_TARGET; VB erased it, and IDLE's StartAttacking then picked
     * the nearest visible player instead (here the nearer watcher, not the original target).
     */
    @GameTest(template = LANE, batch = "garden_fight_freeze", timeoutTicks = 160)
    public static void garden_frozen_creaking_keeps_target(GameTestHelper helper) {
        Creaking creaking = stillCreaking(helper, new Vec3(2.5, 1.0, 2.5));
        ServerPlayer target = GardenTestPlayers.survival(helper, new Vec3(12.5, 1.0, 2.5), 160);
        ServerPlayer watcher = GardenTestPlayers.survival(helper, new Vec3(8.5, 1.0, 1.0), 160);
        GardenTestPlayers.lookAwayFrom(target, creaking.getEyePosition());
        GardenTestPlayers.lookAwayFrom(watcher, creaking.getEyePosition());
        helper.startSequence()
            .thenWaitUntil(() -> {
                check(sensed(creaking, target) && sensed(creaking, watcher), "players not sensed");
                check(creaking.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER).orElse(null) == watcher, "watcher is not the nearest visible player");
            })
            .thenExecute(() -> {
                creaking.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                creaking.activate(target);
            })
            .thenWaitUntil(() -> check(creaking.getBrain().isActive(Activity.FIGHT), "creaking did not start fighting"))
            .thenExecute(() -> GardenTestPlayers.lookAt(watcher, creaking.getEyePosition()))
            .thenWaitUntil(() -> check(!creaking.canMove(), "creaking did not freeze"))
            .thenIdle(5)
            .thenExecute(() -> {
                check(!creaking.canMove(), "creaking unfroze");
                check(creaking.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) == target, "frozen creaking changed or lost its attack target");
                GardenTestPlayers.remove(target);
                GardenTestPlayers.remove(watcher);
            })
            .thenSucceed();
    }

    // ---- GARDEN-04 -------------------------------------------------------------------------------------------

    /** A target behind a wall is not in the visible attackable list, so FIGHT drops it (VB kept chasing). */
    @GameTest(template = LANE, batch = "garden_fight_los", timeoutTicks = 120)
    public static void garden_creaking_drops_target_behind_wall(GameTestHelper helper) {
        for (int y = 1; y < 6; y++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(7, y, z), Blocks.STONE);
            }
        }
        Creaking creaking = stillCreaking(helper, new Vec3(2.5, 1.0, 2.5));
        ServerPlayer player = GardenTestPlayers.survival(helper, new Vec3(12.5, 1.0, 2.5), 120);
        GardenTestPlayers.lookAwayFrom(player, creaking.getEyePosition());
        helper.startSequence()
            .thenWaitUntil(() -> check(sensed(creaking, player), "player 10 blocks away is not sensed"))
            .thenExecute(() -> {
                // Restart the 200-tick "tired of trying to reach" timer so only line of sight can end the chase here.
                creaking.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                creaking.activate(player);
            })
            .thenWaitUntil(() -> {
                check(!player.isRemoved(), "test player already removed");
                check(creaking.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).isEmpty(), "creaking still targets a player it cannot see");
            })
            .thenExecute(() -> GardenTestPlayers.remove(player))
            .thenSucceed();
    }

    // ---- GARDEN-05 -------------------------------------------------------------------------------------------

    /** Replacing the heart with /setblock-style code tears the creaking down at once (native preRemoveSideEffects). */
    @GameTest(template = SMALL)
    public static void garden_heart_removed_by_setblock_crumbles_creaking(GameTestHelper helper) {
        BlockPos heartPos = new BlockPos(3, 2, 3);
        helper.setBlock(heartPos.below(), ModBlocks.PALE_OAK_LOG.get());
        helper.setBlock(heartPos.above(), ModBlocks.PALE_OAK_LOG.get());
        helper.setBlock(heartPos, ModBlocks.CREAKING_HEART.get().defaultBlockState().setValue(CreakingHeartBlock.STATE, CreakingHeartState.AWAKE));
        CreakingHeartBlockEntity heart = helper.getBlockEntity(heartPos);
        Creaking creaking = helper.spawn(ModEntityTypes.CREAKING.get(), new Vec3(1.5, 1.0, 1.5));
        creaking.setTransient(helper.absolutePos(heartPos));
        heart.setCreakingInfo(creaking);
        check(heart.isProtector(creaking), "creaking is not the heart's protector");

        helper.setBlock(heartPos, Blocks.AIR);
        check(creaking.isRemoved(), "creaking was not torn down when its heart was replaced");
        helper.succeed();
    }

    // ---- GARDEN-06 -------------------------------------------------------------------------------------------

    /** Boats push creakings instead of boarding them (#cannot_be_pushed_onto_boats). */
    @GameTest(template = SMALL, timeoutTicks = 60)
    public static void garden_boat_does_not_pick_up_creaking(GameTestHelper helper) {
        Creaking creaking = stillCreaking(helper, new Vec3(3.5, 1.0, 3.5));
        helper.spawn(EntityType.BOAT, new Vec3(3.5, 1.0, 3.5));
        helper.startSequence()
            .thenExecuteFor(40, () -> check(!creaking.isPassenger(), "creaking was pushed into a boat"))
            .thenSucceed();
    }

    /** Control: a zombie still boards a boat, so the boat test above really exercises boarding. */
    @GameTest(template = SMALL, timeoutTicks = 60)
    public static void garden_boat_still_picks_up_zombie(GameTestHelper helper) {
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(3.5, 1.0, 3.5));
        zombie.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0);
        Boat boat = helper.spawn(EntityType.BOAT, new Vec3(3.5, 1.0, 3.5));
        helper.succeedWhen(() -> check(zombie.getVehicle() == boat, "zombie did not board the boat"));
    }

    // ---- GARDEN-07 -------------------------------------------------------------------------------------------

    /** A heart-bound creaking accepts a rider, as native (VB refused all passengers). */
    @GameTest(template = SMALL)
    public static void garden_heart_bound_creaking_accepts_rider(GameTestHelper helper) {
        Creaking creaking = stillCreaking(helper, new Vec3(3.5, 1.0, 3.5));
        creaking.setHomePos(helper.absolutePos(new BlockPos(3, 1, 3)));
        check(creaking.isHeartBound(), "creaking is not heart-bound");
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(3.5, 1.0, 3.5));
        boolean rode = zombie.startRiding(creaking);
        check(rode && creaking.hasPassenger(zombie), "heart-bound creaking refused a rider");
        zombie.discard();
        creaking.discard();
        helper.succeed();
    }

    // ---- GARDEN-13 -------------------------------------------------------------------------------------------

    @GameTest(template = SMALL)
    public static void garden_pale_blocks_map_colour_metal(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        for (Block block : List.of(ModBlocks.PALE_OAK_LEAVES.get(), ModBlocks.PALE_OAK_SAPLING.get(), ModBlocks.CLOSED_EYEBLOSSOM.get())) {
            check(block.defaultMapColor() == MapColor.METAL, block + " default map colour is not METAL");
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                check(state.getMapColor(helper.getLevel(), helper.absolutePos(pos)) == MapColor.METAL, state + " map colour is not METAL");
            }
        }
        helper.succeed();
    }

    // ---- GARDEN-12 / 17 / 10 ---------------------------------------------------------------------------------

    @GameTest(template = SMALL)
    public static void garden_block_properties_match_native(GameTestHelper helper) {
        check(ModBlocks.PALE_MOSS_CARPET.get().defaultBlockState().getSoundType() == SoundType.MOSS_CARPET, "pale moss carpet sound type is not MOSS_CARPET");
        check(((BonemealableBlock) ModBlocks.PALE_MOSS_BLOCK.get()).getType() == BonemealableBlock.Type.NEIGHBOR_SPREADER, "pale moss is not a neighbour spreader");
        check(BuiltInRegistries.PARTICLE_TYPE.get(GardenRegistry.BLOCK_CRUMBLE_ID) == GardenRegistry.BLOCK_CRUMBLE, "minecraft:block_crumble is not registered");
        helper.succeed();
    }

    // ---- GARDEN-14 -------------------------------------------------------------------------------------------

    @GameTest(template = SMALL)
    public static void garden_open_eyeblossom_stays_open_outside_overworld(GameTestHelper helper) {
        for (ResourceKey<Level> key : List.of(Level.NETHER, Level.END)) {
            ServerLevel level = helper.getLevel().getServer().getLevel(key);
            check(level != null, key.location() + " is not loaded");
            BlockPos pos = new BlockPos(0, 100, 0);
            level.getChunk(pos);
            BlockState oldBelow = level.getBlockState(pos.below());
            BlockState old = level.getBlockState(pos);
            level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);
            level.setBlock(pos, ModBlocks.OPEN_EYEBLOSSOM.get().defaultBlockState(), 3);
            RandomSource random = RandomSource.create(1L);
            level.getBlockState(pos).randomTick(level, pos, random);
            level.getBlockState(pos).tick(level, pos, random);
            boolean stillOpen = level.getBlockState(pos).is(ModBlocks.OPEN_EYEBLOSSOM.get());
            level.setBlock(pos, old, 3);
            level.setBlock(pos.below(), oldBelow, 3);
            check(stillOpen, "open eyeblossom changed state in " + key.location());
        }
        helper.succeed();
    }

    // ---- GARDEN-15 / 16 --------------------------------------------------------------------------------------

    @GameTest(template = SMALL)
    public static void garden_bee_food_is_open_eyeblossom_only(GameTestHelper helper) {
        Bee bee = helper.spawnWithNoFreeWill(EntityType.BEE, new Vec3(3.5, 2.0, 3.5));
        check(!bee.isFood(new ItemStack(ModBlocks.CLOSED_EYEBLOSSOM.get())), "closed eyeblossom is bee food");
        check(bee.isFood(new ItemStack(ModBlocks.OPEN_EYEBLOSSOM.get())), "open eyeblossom is not bee food");
        bee.discard();
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void garden_only_open_eyeblossom_poisons_bees(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 1, 3);
        BlockPos abs = helper.absolutePos(pos);
        Bee bee = helper.spawnWithNoFreeWill(EntityType.BEE, new Vec3(3.5, 1.2, 3.5));
        helper.setBlock(pos, ModBlocks.CLOSED_EYEBLOSSOM.get());
        helper.getBlockState(pos).entityInside(helper.getLevel(), abs, bee);
        check(!bee.hasEffect(MobEffects.POISON), "closed eyeblossom poisoned a bee");
        helper.setBlock(pos, ModBlocks.OPEN_EYEBLOSSOM.get());
        helper.getBlockState(pos).entityInside(helper.getLevel(), abs, bee);
        check(bee.hasEffect(MobEffects.POISON), "open eyeblossom did not poison a bee");
        bee.discard();
        helper.succeed();
    }

    // ---- GARDEN-18 -------------------------------------------------------------------------------------------

    @GameTest(template = SMALL)
    public static void garden_hanging_moss_rejects_bonemeal_below_world(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HangingMossBlock moss = (HangingMossBlock) ModBlocks.PALE_HANGING_MOSS.get();
        BlockState state = moss.defaultBlockState();
        BlockPos abs = helper.absolutePos(new BlockPos(3, 0, 3));
        BlockPos bottom = new BlockPos(abs.getX(), level.getMinBuildHeight(), abs.getZ());
        check(!moss.isValidBonemealTarget(level, bottom, state), "hanging moss at the world bottom accepts bone meal");

        BlockPos inAir = new BlockPos(3, 4, 3);
        helper.setBlock(inAir, state);
        check(moss.isValidBonemealTarget(level, helper.absolutePos(inAir), state), "hanging moss with air below rejects bone meal");
        helper.succeed();
    }

    // ---- GARDEN-19 -------------------------------------------------------------------------------------------

    /** simple_block samples its state provider once (VB's injector made it twice for anything but carpets/eyeblossoms). */
    @GameTest(template = SMALL)
    public static void garden_simple_block_feature_samples_once(GameTestHelper helper) {
        AtomicInteger calls = new AtomicInteger();
        BlockStateProvider counting = new BlockStateProvider() {
            @Override
            protected BlockStateProviderType<?> type() {
                return BlockStateProviderType.SIMPLE_STATE_PROVIDER;
            }

            @Override
            public BlockState getState(RandomSource random, BlockPos pos) {
                calls.incrementAndGet();
                return Blocks.SHORT_GRASS.defaultBlockState();
            }
        };
        BlockPos ground = new BlockPos(3, 1, 3);
        helper.setBlock(ground, Blocks.GRASS_BLOCK);
        ServerLevel level = helper.getLevel();
        boolean placed = Feature.SIMPLE_BLOCK.place(new SimpleBlockConfiguration(counting), level, level.getChunkSource().getGenerator(), RandomSource.create(7L), helper.absolutePos(ground.above()));
        check(placed, "simple_block did not place");
        check(calls.get() == 1, "state provider sampled " + calls.get() + " times");
        helper.assertBlockPresent(Blocks.SHORT_GRASS, ground.above());
        helper.succeed();
    }

    // ---- pale garden climate placement -------------------------------------------------------------------------

    /** Pale garden sits where native puts it: PLATEAU_BIOMES[2][4] (formerly dark forest), nowhere else. */
    @GameTest(template = SMALL)
    public static void garden_pale_garden_native_climate(GameTestHelper helper) {
        Climate.ParameterList<Holder<Biome>> params = helper.getLevel().registryAccess()
            .registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
            .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD)
            .parameters();
        // Mid slice, normal (negative) weirdness, far inland, erosion 1: native plateau -> pale garden.
        Holder<Biome> plateau = params.findValue(Climate.target(0.0F, 0.5F, 0.5F, -0.5F, 0.0F, -0.3F));
        check(plateau.is(ModBiomes.PALE_GARDEN), "mid-slice far-inland plateau is " + plateau.getRegisteredName() + ", not pale_garden");
        // Mid slice, variant weirdness, mid inland, erosion 1: native middle biome (dark forest), VB added pale garden here.
        Holder<Biome> middle = params.findValue(Climate.target(0.0F, 0.5F, 0.15F, -0.5F, 0.0F, 0.3F));
        check(!middle.is(ModBiomes.PALE_GARDEN), "mid-inland middle biome is pale_garden");
        for (com.mojang.datafixers.util.Pair<Climate.ParameterPoint, Holder<Biome>> entry : params.values()) {
            if (entry.getSecond().is(ModBiomes.PALE_GARDEN)) {
                Climate.ParameterPoint point = entry.getFirst();
                check(point.temperature().equals(Climate.Parameter.span(-0.15F, 0.2F)) && point.humidity().equals(Climate.Parameter.span(0.3F, 1.0F)),
                    "pale garden point outside neutral/humid: " + point);
            }
        }
        helper.succeed();
    }
}
