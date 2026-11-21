package com.coolfreeze23.vbparity.copper;

import com.blackgear.vanillabackport.common.level.block_entities.ShelfBlockEntity;
import com.blackgear.vanillabackport.common.level.blocks.CopperGolemStatueBlock;
import com.blackgear.vanillabackport.common.level.blocks.ShelfBlock;
import com.blackgear.vanillabackport.common.level.components.KineticWeapon;
import com.blackgear.vanillabackport.common.level.entities.ai.goal.OfferCopperGolemFlowerGoal;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.golem.copper_golem.CopperGolem;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.golem.copper_golem.CopperGolemPathNavigation;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.golem.copper_golem.container.ContainerManager;
import com.blackgear.vanillabackport.common.registries.blocks.ModBlocks;
import com.blackgear.vanillabackport.common.registries.entities.ModEntityTypes;
import com.blackgear.vanillabackport.common.registries.items.ModArmorMaterials;
import com.blackgear.vanillabackport.common.registries.items.ModDataComponents;
import com.blackgear.vanillabackport.common.registries.items.ModItems;
import com.blackgear.vanillabackport.core.data.tags.ModBlockTags;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.OfferFlowerGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vbparity")
@PrefixGameTestTemplate(false)
public final class CopperGameTests {
    private static final String SMALL = "copper_small";
    private static final String LONG = "copper_long";

    // ---------------------------------------------------------------- golem navigation (COPPER-01, 02)

    @GameTest(template = LONG, timeoutTicks = 60)
    public static void copper_golem_path_length_48(GameTestHelper helper) {
        CopperGolem golem = helper.spawn(ModEntityTypes.COPPER_GOLEM.get(), new BlockPos(1, 2, 2));
        golem.setNoAi(true);
        golem.setOnGround(true); // a no-AI mob never moves, so it never sets onGround itself
        BlockPos target = helper.absolutePos(new BlockPos(41, 2, 2));
        helper.succeedWhen(() -> {
            Path path = golem.getNavigation().createPath(target, 0);
            helper.assertTrue(path != null && path.canReach(), "no full path to a target 40 blocks away (native required path length is 48)");
        });
    }

    @GameTest(template = SMALL, timeoutTicks = 60)
    public static void copper_golem_find_surface_position(GameTestHelper helper) {
        helper.setBlock(new BlockPos(5, 2, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 3, 5), Blocks.STONE);
        CopperGolem golem = helper.spawn(ModEntityTypes.COPPER_GOLEM.get(), new BlockPos(1, 2, 1));
        golem.setNoAi(true);
        golem.setOnGround(true);
        BlockPos solidTarget = helper.absolutePos(new BlockPos(5, 2, 5));
        BlockPos surface = helper.absolutePos(new BlockPos(5, 4, 5));
        CopperGolemPathNavigation navigation = (CopperGolemPathNavigation) golem.getNavigation();
        helper.succeedWhen(() -> {
            navigation.stop();
            navigation.setCanPathToTargetsBelowSurface(false);
            Path path = navigation.createPath(solidTarget, 0);
            helper.assertTrue(path != null && surface.equals(path.getTarget()), "surface target " + (path == null ? null : path.getTarget()) + ", expected the first non-solid block " + surface);
            // Transporting golems path to the chest itself (below-surface targets on): native keeps the target as given.
            navigation.setCanPathToTargetsBelowSurface(true);
            Path below = navigation.createPath(solidTarget, 1);
            navigation.setCanPathToTargetsBelowSurface(false);
            helper.assertTrue(below != null && solidTarget.equals(below.getTarget()),
                "below-surface target moved to " + (below == null ? null : below.getTarget()) + ", native keeps " + solidTarget);
        });
    }

    // ---------------------------------------------------------------- chest openers (COPPER-03)

    private static CopperGolem golemHoldingChestOpen(GameTestHelper helper, BlockPos chestRel) {
        helper.setBlock(chestRel, ModBlocks.COPPER_CHEST.get().defaultBlockState());
        CopperGolem golem = helper.spawn(ModEntityTypes.COPPER_GOLEM.get(), chestRel.east(2));
        golem.setNoAi(true);
        BlockPos chest = helper.absolutePos(chestRel);
        golem.setOpenedChestPos(chest);
        ContainerManager.startOpen(helper.getLevel().getBlockEntity(chest), golem);
        helper.assertTrue(ChestBlockEntity.getOpenCount(helper.getLevel(), chest) == 1, "golem did not open the chest");
        return golem;
    }

    @GameTest(template = SMALL)
    public static void copper_chest_closes_after_interrupted_golem(GameTestHelper helper) {
        BlockPos chestRel = new BlockPos(2, 2, 2);
        CopperGolem golem = golemHoldingChestOpen(helper, chestRel);
        golem.clearOpenedChestPos(); // interrupted: hurt, leashed, killed or unloaded before stopOpen
        BlockPos chest = helper.absolutePos(chestRel);
        helper.succeedWhen(() -> helper.assertTrue(
            ChestBlockEntity.getOpenCount(helper.getLevel(), chest) == 0, "chest still counts an opener after the golem stopped"
        ));
    }

    @GameTest(template = SMALL)
    public static void copper_chest_recheck_counts_golem(GameTestHelper helper) {
        BlockPos chestRel = new BlockPos(2, 2, 2);
        CopperGolem golem = golemHoldingChestOpen(helper, chestRel);
        BlockPos chest = helper.absolutePos(chestRel);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(ChestBlockEntity.getOpenCount(helper.getLevel(), chest) == 1, "recheck dropped the golem that still holds the chest open");
            golem.clearOpenedChestPos();
            helper.succeed();
        });
    }

    // ---------------------------------------------------------------- copper chests (COPPER-05, 06)

    @GameTest(template = SMALL)
    public static void copper_waxed_golem_builds_waxed_chest(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.WAXED_EXPOSED_COPPER);
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.CARVED_PUMPKIN);
        helper.assertEntityPresent(ModEntityTypes.COPPER_GOLEM.get());
        BlockState chest = helper.getBlockState(new BlockPos(4, 2, 4));
        helper.assertTrue(chest.is(ModBlocks.WAXED_EXPOSED_COPPER_CHEST.get()), "waxed exposed copper built " + chest + ", expected waxed_exposed_copper_chest");
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void copper_mixed_oxidation_chests_pair(GameTestHelper helper) {
        BlockPos oldRel = new BlockPos(2, 2, 2);
        BlockPos newRel = new BlockPos(3, 2, 2);
        helper.setBlock(oldRel, ModBlocks.WEATHERED_COPPER_CHEST.get().defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));

        Player player = helper.makeMockPlayer(GameType.CREATIVE); // yRot 0: faces south, so the chest faces north
        ItemStack stack = new ItemStack(ModBlocks.COPPER_CHEST.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos floor = helper.absolutePos(newRel.below());
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));

        BlockState placed = helper.getBlockState(newRel);
        BlockState partner = helper.getBlockState(oldRel);
        helper.assertTrue(placed.is(ModBlocks.COPPER_CHEST.get()), "placed chest is " + placed);
        helper.assertTrue(placed.getValue(ChestBlock.TYPE) != ChestType.SINGLE, "a copper chest placed next to a weathered one did not pair: " + placed);
        helper.assertTrue(partner.is(ModBlocks.COPPER_CHEST.get()), "the weathered half did not become the least-oxidized chest: " + partner);
        helper.assertTrue(partner.getValue(ChestBlock.TYPE) == placed.getValue(ChestBlock.TYPE).getOpposite(), "halves do not match: " + partner + " / " + placed);
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void copper_waxed_and_unwaxed_chests_pair_unwaxed(GameTestHelper helper) {
        BlockPos oldRel = new BlockPos(2, 2, 2);
        BlockPos newRel = new BlockPos(3, 2, 2);
        helper.setBlock(oldRel, ModBlocks.WEATHERED_COPPER_CHEST.get().defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH));
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        ItemStack stack = new ItemStack(ModBlocks.WAXED_COPPER_CHEST.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos floor = helper.absolutePos(newRel.below());
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false)));
        BlockState placed = helper.getBlockState(newRel);
        BlockState partner = helper.getBlockState(oldRel);
        // Native unwaxes both halves when only one is waxed, then takes the least oxidized: unwaxed copper chest.
        helper.assertTrue(placed.is(ModBlocks.COPPER_CHEST.get()) && placed.getValue(ChestBlock.TYPE) != ChestType.SINGLE, "placed waxed chest became " + placed);
        helper.assertTrue(partner.is(ModBlocks.COPPER_CHEST.get()), "weathered half became " + partner);
        helper.succeed();
    }

    // ---------------------------------------------------------------- lightning rods (COPPER-08, 09, 10, 11)

    @GameTest(template = SMALL, skyAccess = true, batch = "vbp_copper_rod_attract")
    public static void copper_plain_lightning_rod_attracts(GameTestHelper helper) throws Exception {
        BlockPos rodRel = new BlockPos(4, 2, 4);
        helper.setBlock(rodRel, Blocks.LIGHTNING_ROD);
        Method find = ServerLevel.class.getDeclaredMethod("findLightningTargetAround", BlockPos.class);
        find.setAccessible(true);
        BlockPos expected = helper.absolutePos(rodRel).above();
        BlockPos center = helper.absolutePos(new BlockPos(8, 2, 8));
        helper.runAfterDelay(3, () -> { // POI registration runs on the next server task
            try {
                BlockPos target = (BlockPos) find.invoke(helper.getLevel(), center);
                helper.assertTrue(expected.equals(target), "lightning went to " + target + " instead of the plain lightning rod at " + expected);
                helper.succeed();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static LightningBolt strike(GameTestHelper helper, BlockPos rodRel) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(helper.getLevel());
        bolt.moveTo(Vec3.atBottomCenterOf(helper.absolutePos(rodRel.above())));
        bolt.tick(); // life 2: the strike (powerLightningRod, clearCopperOnLightningStrike) happens on the first tick
        return bolt;
    }

    @GameTest(template = SMALL)
    public static void copper_struck_rod_is_cleaned(GameTestHelper helper) {
        BlockPos rodRel = new BlockPos(4, 2, 4);
        helper.setBlock(rodRel, ModBlocks.OXIDIZED_LIGHTNING_ROD.get().defaultBlockState());
        strike(helper, rodRel);
        BlockState rod = helper.getBlockState(rodRel);
        helper.assertTrue(rod.is(Blocks.LIGHTNING_ROD), "struck oxidized rod is " + rod + ", native resets it to lightning_rod");
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void copper_struck_waxed_rod_cleans_around(GameTestHelper helper) {
        BlockPos rodRel = new BlockPos(4, 3, 4);
        for (int x = 3; x <= 5; x++) {
            for (int y = 2; y <= 3; y++) {
                for (int z = 3; z <= 5; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.OXIDIZED_COPPER);
                }
            }
        }
        helper.setBlock(rodRel.below(), Blocks.STONE); // not copper: VB only ever cleaned the block a rod is attached to
        helper.setBlock(rodRel, ModBlocks.WAXED_LIGHTNING_ROD.get().defaultBlockState());
        strike(helper, rodRel);
        helper.assertTrue(helper.getBlockState(rodRel).is(ModBlocks.WAXED_LIGHTNING_ROD.get()), "waxed rod changed");
        int cleaned = 0;
        for (int x = 3; x <= 5; x++) {
            for (int y = 2; y <= 3; y++) {
                for (int z = 3; z <= 5; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (!p.equals(rodRel) && !p.equals(rodRel.below()) && !helper.getBlockState(p).is(Blocks.OXIDIZED_COPPER)) {
                        cleaned++;
                    }
                }
            }
        }
        helper.assertTrue(cleaned > 0, "a strike on a waxed rod cleaned no copper around it");
        helper.succeed();
    }

    @GameTest(template = SMALL, batch = "vbp_copper_rod_packets")
    public static void copper_struck_rod_sparks_once(GameTestHelper helper) {
        BlockPos rodRel = new BlockPos(4, 2, 4);
        helper.setBlock(rodRel, Blocks.LIGHTNING_ROD);
        BlockPos rod = helper.absolutePos(rodRel);
        int strikes;
        try (PacketCapture capture = new PacketCapture(helper.getLevel(), Vec3.atCenterOf(rod))) {
            strike(helper, rodRel);
            strikes = (int) capture.of(ClientboundLevelEventPacket.class).stream()
                .filter(p -> p.getType() == 3002 && p.getData() != -1 && p.getPos().equals(rod))
                .count();
        }
        helper.assertTrue(strikes == 1, "onLightningStrike spark event sent " + strikes + " times, native sends it once");
        helper.succeed();
    }

    @GameTest(template = SMALL, skyAccess = true, batch = "vbp_copper_channeling")
    public static void copper_channeling_on_weathered_rod(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rodRel = new BlockPos(4, 2, 4);
        helper.setBlock(rodRel, ModBlocks.WEATHERED_LIGHTNING_ROD.get().defaultBlockState());
        BlockPos rod = helper.absolutePos(rodRel);
        BlockState rodState = level.getBlockState(rod);
        Vec3 hit = Vec3.atCenterOf(rod);

        ItemStack tridentItem = new ItemStack(Items.TRIDENT);
        tridentItem.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.CHANNELING), 1);
        ThrownTrident trident = new ThrownTrident(level, hit.x, hit.y + 0.5, hit.z, tridentItem);

        float rain = level.getRainLevel(1.0F);
        float thunder = level.getThunderLevel(1.0F);
        try {
            level.setRainLevel(1.0F);
            level.setThunderLevel(1.0F);
            helper.assertTrue(level.isThundering(), "could not make it thunder");
            EnchantmentHelper.onHitBlock(level, tridentItem, null, trident, null, hit, rodState, item -> {});
            rodState.onProjectileHit(level, rodState, new BlockHitResult(hit, Direction.UP, rod, false), trident);
        } finally {
            level.setRainLevel(rain);
            level.setThunderLevel(thunder);
        }

        List<LightningBolt> bolts = level.getEntitiesOfClass(LightningBolt.class, new AABB(rod).inflate(3.0));
        bolts.forEach(LightningBolt::discard);
        helper.assertTrue(bolts.size() == 1, "channeling summoned " + bolts.size() + " bolts, native summons one");
        helper.assertTrue(bolts.get(0).position().distanceTo(hit) < 1.0E-6, "bolt at " + bolts.get(0).position() + ", native summons it at the hit " + hit);
        helper.succeed();
    }

    // ---------------------------------------------------------------- golem interactions (COPPER-07, 32)

    @GameTest(template = SMALL, batch = "vbp_copper_wax_packets")
    public static void copper_golem_wax_sound_once(GameTestHelper helper) {
        CopperGolem golem = helper.spawn(ModEntityTypes.COPPER_GOLEM.get(), new BlockPos(4, 2, 4));
        golem.setNoAi(true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.HONEYCOMB));
        long sounds;
        long events;
        try (PacketCapture capture = new PacketCapture(helper.getLevel(), golem.position())) {
            golem.interact(player, InteractionHand.MAIN_HAND);
            sounds = capture.of(ClientboundSoundPacket.class).stream().filter(p -> p.getSound().value() == SoundEvents.HONEYCOMB_WAX_ON).count();
            events = capture.of(ClientboundLevelEventPacket.class).stream().filter(p -> p.getType() == 3003).count();
        }
        helper.assertTrue(events == 1, "wax level event sent " + events + " times");
        helper.assertTrue(sounds == 0, "server also sent " + sounds + " wax_on sound packets; level event 3003 already plays it");
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void copper_golem_shearable_while_dying(GameTestHelper helper) {
        CopperGolem golem = helper.spawn(ModEntityTypes.COPPER_GOLEM.get(), new BlockPos(4, 2, 4));
        golem.setNoAi(true);
        golem.setItemSlot(com.coolfreeze23.vbparity.copper.CopperAntenna.SLOT, new ItemStack(Items.POPPY));
        golem.setHealth(0.0F);
        helper.assertFalse(golem.isAlive(), "golem still alive");
        helper.assertTrue(golem.readyForShearing(), "dying golem with a poppy is not shearable (native only checks the antenna item)");
        golem.discard();
        helper.succeed();
    }

    // ---------------------------------------------------------------- statue + shelf comparators (COPPER-12, 13)

    private static int comparatorOutput(GameTestHelper helper, BlockPos rel) {
        return ((ComparatorBlockEntity) helper.getBlockEntity(rel)).getOutputSignal();
    }

    @GameTest(template = SMALL)
    public static void copper_statue_comparator_output(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockState statue = ModBlocks.COPPER_GOLEM_STATUE.get().defaultBlockState();
        for (CopperGolemStatueBlock.Pose pose : CopperGolemStatueBlock.Pose.values()) {
            int signal = statue.setValue(CopperGolemStatueBlock.POSE, pose).getAnalogOutputSignal(level, helper.absolutePos(new BlockPos(1, 2, 1)));
            helper.assertTrue(signal == pose.ordinal() + 1, "pose " + pose + " gives " + signal + ", native " + (pose.ordinal() + 1));
        }

        BlockPos statueRel = new BlockPos(1, 2, 1);
        BlockPos comparatorRel = new BlockPos(3, 2, 1);
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.STONE);
        helper.setBlock(comparatorRel, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST));
        helper.setBlock(statueRel, statue.setValue(CopperGolemStatueBlock.POSE, CopperGolemStatueBlock.Pose.STAR));
        level.updateNeighbourForOutputSignal(helper.absolutePos(statueRel), statue.getBlock());
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(comparatorOutput(helper, comparatorRel) == 4, "star statue: comparator through a block reads " + comparatorOutput(helper, comparatorRel)))
            .thenExecute(() -> helper.setBlock(statueRel, Blocks.AIR))
            .thenWaitUntil(() -> helper.assertTrue(comparatorOutput(helper, comparatorRel) == 0, "comparator not updated when the statue was removed"))
            .thenSucceed();
    }

    @GameTest(template = SMALL)
    public static void copper_shelf_comparator_back_only(GameTestHelper helper) {
        BlockPos shelfRel = new BlockPos(4, 2, 4);
        helper.setBlock(shelfRel, ModBlocks.OAK_SHELF.get().defaultBlockState().setValue(ShelfBlock.FACING, Direction.NORTH));
        ((ShelfBlockEntity) helper.getBlockEntity(shelfRel)).setItem(0, new ItemStack(Items.STICK));
        BlockPos back = new BlockPos(4, 2, 5);
        BlockPos side = new BlockPos(5, 2, 4);
        BlockPos front = new BlockPos(4, 2, 3);
        helper.setBlock(back, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.NORTH));
        helper.setBlock(side, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST));
        helper.setBlock(front, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.SOUTH));
        helper.getLevel().updateNeighbourForOutputSignal(helper.absolutePos(shelfRel), ModBlocks.OAK_SHELF.get());
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(comparatorOutput(helper, back) == 1, "comparator behind the shelf reads " + comparatorOutput(helper, back)))
            .thenIdle(4)
            .thenExecute(() -> {
                helper.assertTrue(comparatorOutput(helper, side) == 0, "comparator beside the shelf reads " + comparatorOutput(helper, side) + ", native 0");
                helper.assertTrue(comparatorOutput(helper, front) == 0, "comparator in front of the shelf reads " + comparatorOutput(helper, front) + ", native 0");
            })
            .thenSucceed();
    }

    // ---------------------------------------------------------------- shelf fuel (COPPER-15)

    @GameTest(template = SMALL)
    public static void copper_shelf_fuel_values(GameTestHelper helper) {
        List<Supplier<Block>> burning = List.of(
            ModBlocks.ACACIA_SHELF, ModBlocks.BAMBOO_SHELF, ModBlocks.BIRCH_SHELF, ModBlocks.CHERRY_SHELF, ModBlocks.DARK_OAK_SHELF, ModBlocks.JUNGLE_SHELF,
            ModBlocks.MANGROVE_SHELF, ModBlocks.OAK_SHELF, ModBlocks.PALE_OAK_SHELF, ModBlocks.SPRUCE_SHELF
        );
        for (Supplier<Block> shelf : burning) {
            int burn = new ItemStack(shelf.get()).getBurnTime(RecipeType.SMELTING);
            helper.assertTrue(burn == 300, BuiltInRegistries.BLOCK.getKey(shelf.get()) + " burns " + burn + " ticks, native 300");
        }
        for (Supplier<Block> shelf : List.of(ModBlocks.CRIMSON_SHELF, ModBlocks.WARPED_SHELF)) {
            int burn = new ItemStack(shelf.get()).getBurnTime(RecipeType.SMELTING);
            helper.assertTrue(burn == 0, BuiltInRegistries.BLOCK.getKey(shelf.get()) + " burns " + burn + " ticks, native 0");
        }
        helper.succeed();
    }

    // ---------------------------------------------------------------- mob armor (COPPER-19)

    @GameTest(template = SMALL)
    public static void copper_mob_armor_tiers(GameTestHelper helper) throws Exception {
        Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
        Method populate = Mob.class.getDeclaredMethod("populateDefaultEquipmentSlots", RandomSource.class, DifficultyInstance.class);
        populate.setAccessible(true);
        RandomSource random = RandomSource.create(20261122L);
        DifficultyInstance difficulty = new DifficultyInstance(Difficulty.HARD, 10_000_000L, 10_000_000L, 1.0F);
        List<EquipmentSlot> slots = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
        Map<Item, Integer> tierOf = Map.ofEntries(
            Map.entry(Items.LEATHER_HELMET, 0), Map.entry(ModItems.COPPER_HELMET.get(), 1), Map.entry(Items.GOLDEN_HELMET, 2),
            Map.entry(Items.CHAINMAIL_HELMET, 3), Map.entry(Items.IRON_HELMET, 4), Map.entry(Items.DIAMOND_HELMET, 5)
        );
        int[] counts = new int[6];
        int armored = 0;
        for (int trial = 0; trial < 120_000; trial++) {
            for (EquipmentSlot slot : slots) {
                zombie.setItemSlot(slot, ItemStack.EMPTY);
            }
            populate.invoke(zombie, random, difficulty);
            boolean any = slots.stream().anyMatch(s -> !zombie.getItemBySlot(s).isEmpty());
            if (!any) {
                continue;
            }
            armored++;
            ItemStack helmet = zombie.getItemBySlot(EquipmentSlot.HEAD);
            helper.assertTrue(!helmet.isEmpty(), "armored mob without a helmet: native fills HEAD first");
            counts[tierOf.get(helmet.getItem())]++;
        }
        zombie.discard();
        helper.assertTrue(armored > 10_000, "only " + armored + " armored mobs");
        double leather = counts[0] / (double) armored;
        double copper = counts[1] / (double) armored;
        double gold = counts[2] / (double) armored;
        String dist = String.format("leather %.3f copper %.3f gold %.3f (n=%d)", leather, copper, gold, armored);
        helper.assertTrue(leather > 0.22 && leather < 0.255, "leather share off native 0.236: " + dist);
        helper.assertTrue(copper > 0.305 && copper < 0.34, "copper share off native 0.322: " + dist);
        helper.assertTrue(gold > 0.315 && gold < 0.35, "gold share off native 0.333: " + dist);
        helper.succeed();
    }

    // ---------------------------------------------------------------- data (COPPER-04, 16, 18, 20, 21, 29, 30, 31, 33)

    @GameTest(template = SMALL)
    public static void copper_trim_palette_on_copper_armor(GameTestHelper helper) {
        Holder<TrimMaterial> copper = helper.getLevel().registryAccess().registryOrThrow(Registries.TRIM_MATERIAL).getHolderOrThrow(TrimMaterials.COPPER);
        Map<Holder<ArmorMaterial>, String> overrides = copper.value().overrideArmorMaterials();
        String suffix = overrides.get(ModArmorMaterials.COPPER);
        helper.assertTrue("copper_darker".equals(suffix), "copper trim on copper armor uses palette " + suffix + ", native copper_darker");
        helper.succeed();
    }

    private static Recipe<?> recipe(GameTestHelper helper, String id) {
        Optional<RecipeHolder<?>> holder = helper.getLevel().getRecipeManager().byKey(ResourceLocation.withDefaultNamespace(id));
        helper.assertTrue(holder.isPresent(), "missing recipe minecraft:" + id);
        return holder.get().value();
    }

    @GameTest(template = SMALL)
    public static void copper_honeycomb_recipes(GameTestHelper helper) {
        for (String kind : List.of("bars", "chain", "lantern")) {
            for (String stage : List.of("", "exposed_", "weathered_", "oxidized_")) {
                String source = stage + "copper_" + kind;
                Recipe<?> r = recipe(helper, "waxed_" + source + "_from_honeycomb");
                ItemStack result = r.getResultItem(helper.getLevel().registryAccess());
                Item expected = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("waxed_" + source));
                helper.assertTrue(result.is(expected) && result.getCount() == 1, "waxed_" + source + "_from_honeycomb gives " + result);
                helper.assertTrue(r.getIngredients().size() == 2
                    && r.getIngredients().get(0).test(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(source))))
                    && r.getIngredients().get(1).test(new ItemStack(Items.HONEYCOMB)), "bad ingredients for " + source);
                String group = "waxed_copper_" + (kind.equals("bars") ? "bar" : kind);
                helper.assertTrue(group.equals(r.getGroup()), source + " group " + r.getGroup() + ", native " + group);
            }
        }
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void copper_waxed_recipe_groups(GameTestHelper helper) {
        Map<String, String> families = Map.of("copper_chest", "waxed_copper_chest", "copper_golem_statue", "waxed_copper_golem_statue", "lightning_rod", "waxed_lightning_rod");
        for (String stage : List.of("", "exposed_", "weathered_", "oxidized_")) {
            families.forEach((family, group) -> {
                String id = "waxed_" + stage + family + "_from_honeycomb";
                String actual = recipe(helper, id).getGroup();
                helper.assertTrue(group.equals(actual), id + " group " + actual + ", native " + group);
            });
        }
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void copper_nugget_recipes_use_the_item(GameTestHelper helper) {
        for (String id : List.of("copper_chain", "copper_lantern", "copper_torch")) {
            for (Ingredient ingredient : recipe(helper, id).getIngredients()) {
                if (ingredient.isCustom() || ingredient.isEmpty()) {
                    continue;
                }
                for (Ingredient.Value value : ingredient.getValues()) {
                    helper.assertFalse(value instanceof Ingredient.TagValue tag && tag.tag().location().getPath().equals("nuggets/copper"),
                        id + " still accepts #c:nuggets/copper; native uses minecraft:copper_nugget");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void copper_transport_destinations_are_vanilla_chests(GameTestHelper helper) {
        for (Holder<Block> block : BuiltInRegistries.BLOCK.getTagOrEmpty(ModBlockTags.TRANSPORT_ITEM_DESTINATION_BLOCKS)) {
            helper.assertTrue(block.value() == Blocks.CHEST || block.value() == Blocks.TRAPPED_CHEST,
                "golem destination tag contains " + block.unwrapKey().map(ResourceKey::location).orElse(null) + "; native allows only chest and trapped_chest");
        }
        helper.assertTrue(Blocks.CHEST.defaultBlockState().is(ModBlockTags.TRANSPORT_ITEM_DESTINATION_BLOCKS), "chest missing from destinations");
        helper.assertTrue(Blocks.TRAPPED_CHEST.defaultBlockState().is(ModBlockTags.TRANSPORT_ITEM_DESTINATION_BLOCKS), "trapped chest missing from destinations");
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void copper_chests_guarded_by_piglins(GameTestHelper helper) {
        for (Supplier<Block> chest : List.of(ModBlocks.COPPER_CHEST, ModBlocks.OXIDIZED_COPPER_CHEST, ModBlocks.WAXED_WEATHERED_COPPER_CHEST)) {
            helper.assertTrue(chest.get().defaultBlockState().is(BlockTags.GUARDED_BY_PIGLINS), BuiltInRegistries.BLOCK.getKey(chest.get()) + " is not guarded by piglins");
        }
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void copper_spear_dismount_threshold(GameTestHelper helper) {
        KineticWeapon kinetic = new ItemStack(ModItems.COPPER_SPEAR.get()).get(ModDataComponents.KINETIC_WEAPON.get());
        helper.assertTrue(kinetic != null && kinetic.dismountConditions().isPresent(), "copper spear has no dismount condition");
        KineticWeapon.Condition dismount = kinetic.dismountConditions().get();
        helper.assertTrue(dismount.minSpeed() == 12.0F, "copper spear dismount threshold " + dismount.minSpeed() + ", native 12.0");
        helper.assertTrue(dismount.maxDurationTicks() == 80, "copper spear dismount time " + dismount.maxDurationTicks() + " ticks, native 80");
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void copper_statue_item_default_pose(GameTestHelper helper) {
        for (Supplier<Block> statue : CopperParityEvents.statues()) {
            ItemStack stack = new ItemStack(statue.get());
            var props = stack.get(DataComponents.BLOCK_STATE);
            helper.assertTrue(props != null && "standing".equals(props.properties().get("copper_golem_pose")),
                BuiltInRegistries.BLOCK.getKey(statue.get()) + " item has block_state " + props + ", native {copper_golem_pose: standing}");
        }
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void copper_loot_tables_random_sequence(GameTestHelper helper) throws Exception {
        Field field = LootTable.class.getDeclaredField("randomSequence");
        field.setAccessible(true);
        for (String id : List.of("blocks/copper_chest", "blocks/waxed_oxidized_copper_golem_statue", "blocks/oak_shelf", "blocks/exposed_lightning_rod",
            "blocks/copper_lantern", "entities/copper_golem")) {
            LootTable table = helper.getLevel().getServer().reloadableRegistries()
                .getLootTable(ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace(id)));
            @SuppressWarnings("unchecked")
            Optional<ResourceLocation> sequence = (Optional<ResourceLocation>) field.get(table);
            helper.assertTrue(sequence.isPresent() && sequence.get().equals(ResourceLocation.withDefaultNamespace(id)),
                "loot table minecraft:" + id + " random_sequence " + sequence + ", native minecraft:" + id);
        }
        helper.succeed();
    }

    // ---------------------------------------------------------------- iron golem flower (COPPER-22)

    @GameTest(template = SMALL, batch = "vbp_copper_flower", timeoutTicks = 40)
    public static void copper_iron_golem_single_flower_goal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        level.setDayTime(1000L);
        level.updateSkyBrightness();
        IronGolem golem = helper.spawn(EntityType.IRON_GOLEM, new BlockPos(3, 2, 4));
        golem.setNoAi(true);
        Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(6, 2, 4));
        villager.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(level.isDay(), "not day");
            Goal vanilla = null;
            Goal copper = null;
            for (WrappedGoal wrapped : golem.goalSelector.getAvailableGoals()) {
                if (wrapped.getGoal() instanceof OfferFlowerGoal) {
                    vanilla = wrapped.getGoal();
                } else if (wrapped.getGoal() instanceof OfferCopperGolemFlowerGoal) {
                    copper = wrapped.getGoal();
                }
            }
            helper.assertTrue(vanilla != null, "iron golem has no OfferFlowerGoal");
            int vanillaHits = 0;
            int copperHits = 0;
            for (int i = 0; i < 400_000; i++) {
                if (vanilla.canUse()) {
                    vanillaHits++;
                }
                if (copper != null && copper.canUse()) {
                    copperHits++;
                }
            }
            helper.assertTrue(copperHits == 0, "second flower goal still rolls: " + copperHits + " offers");
            // One goal, one nextInt(8000) roll per check: about 50 offers in 400000 checks, and the villager is a candidate.
            helper.assertTrue(vanillaHits >= 20 && vanillaHits <= 90, "flower goal offered " + vanillaHits + " times in 400000 checks, native about 50");
            helper.succeed();
        });
    }
}
